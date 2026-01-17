package io.tugrandsolutions.flowforge.workflow.orchestrator;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.task.TaskResult;
import io.tugrandsolutions.flowforge.workflow.InMemoryReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.input.DefaultTaskInputResolver;
import io.tugrandsolutions.flowforge.workflow.input.TaskInputResolver;
import io.tugrandsolutions.flowforge.workflow.instance.TaskStatus;
import io.tugrandsolutions.flowforge.workflow.instance.WorkflowInstance;
import io.tugrandsolutions.flowforge.workflow.instance.WorkflowRunMetadata;
import io.tugrandsolutions.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.monitor.WorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Fully event-driven orchestrator.
 *
 * <p>
 * Design goals:
 * <ul>
 * <li>No polling loops: new work is scheduled only when the state changes.</li>
 * <li>Exactly-once execution: a task can only transition READY -> RUNNING
 * once.</li>
 * <li>Single-writer state: all WorkflowInstance mutations happen on a dedicated
 * single scheduler.</li>
 * <li>Safe multi-producer event emission: worker completions can arrive
 * concurrently.</li>
 * </ul>
 */
public final class ReactiveWorkflowOrchestrator {

    /**
     * Reactor sinks are not generally safe for concurrent tryEmitNext without
     * handling.
     * We retry on FAIL_NON_SERIALIZED which is the expected transient failure for
     * concurrent emissions.
     */
    private static final Sinks.EmitFailureHandler RETRY_NON_SERIALIZED = (signalType,
            emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED;

    private static final Object NULL_SENTINEL = new Object();

    private final Scheduler taskScheduler;
    private final Scheduler stateScheduler;
    private final WorkflowMonitor monitor;
    private final TaskInputResolver inputResolver;
    private final int maxConcurrency;
    private final boolean ownsStateScheduler;

    // Timing tracking for observability
    private final java.util.concurrent.ConcurrentHashMap<TaskId, Long> taskStartTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<TaskId, java.time.Duration> taskDurations = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<TaskId, Throwable> taskErrors = new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicInteger maxInFlightObserved = new AtomicInteger(0);

    public ReactiveWorkflowOrchestrator() {
        this(
                Schedulers.boundedElastic(),
                new NoOpWorkflowMonitor(),
                new DefaultTaskInputResolver());
    }

    /**
     * Compatibility constructor (used by tests).
     */
    public ReactiveWorkflowOrchestrator(
            Scheduler taskScheduler,
            WorkflowMonitor monitor,
            TaskInputResolver inputResolver) {
        this(
                taskScheduler,
                Schedulers.newSingle("wf-state"),
                monitor,
                inputResolver,
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                true);
    }

    public ReactiveWorkflowOrchestrator(
            Scheduler taskScheduler,
            Scheduler stateScheduler,
            WorkflowMonitor monitor,
            TaskInputResolver inputResolver,
            int maxConcurrency) {
        this(taskScheduler, stateScheduler, monitor, inputResolver, maxConcurrency, false);
    }

    private ReactiveWorkflowOrchestrator(
            Scheduler taskScheduler,
            Scheduler stateScheduler,
            WorkflowMonitor monitor,
            TaskInputResolver inputResolver,
            int maxConcurrency,
            boolean ownsStateScheduler) {
        this.taskScheduler = taskScheduler;
        this.stateScheduler = stateScheduler;
        this.monitor = monitor;
        this.inputResolver = inputResolver;
        this.maxConcurrency = maxConcurrency;
        this.ownsStateScheduler = ownsStateScheduler;
    }

    public Mono<ReactiveExecutionContext> execute(String workflowId, WorkflowExecutionPlan plan, Object initialInput) {
        return execute(WorkflowRunMetadata.of(workflowId), plan, initialInput);
    }

    public Mono<ReactiveExecutionContext> execute(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            Object initialInput) {
        ReactiveExecutionContext context = new InMemoryReactiveExecutionContext();
        WorkflowInstance instance = new WorkflowInstance(metadata, plan, context);

        monitor.onWorkflowStart(instance);

        return executeEventDriven(instance, initialInput)
                .materialize()
                .doOnNext(signal -> {
                    // Build execution report synchronously within the flow
                    io.tugrandsolutions.flowforge.workflow.report.ExecutionReport report = buildExecutionReport(
                            instance);
                    monitor.onWorkflowComplete(instance, report);
                })
                .dematerialize()
                .doOnCancel(() -> {
                    io.tugrandsolutions.flowforge.workflow.report.ExecutionReport report = buildExecutionReport(
                            instance);
                    monitor.onWorkflowComplete(instance, report);
                })
                .doFinally(sig -> {
                    if (ownsStateScheduler) {
                        stateScheduler.dispose();
                    }
                })
                .thenReturn(context);
    }

    /**
     * @deprecated Use {@link #execute(String, WorkflowExecutionPlan, Object)}
     *             instead.
     */
    @Deprecated
    public Mono<ReactiveExecutionContext> execute(WorkflowExecutionPlan plan, Object initialInput) {
        return execute("UNKNOWN", plan, initialInput);
    }

    /**
     * Executes the workflow with a global timeout.
     * If the workflow does not complete within the specified duration, the returned
     * Mono
     * will emit a {@link java.util.concurrent.TimeoutException} and the workflow
     * execution
     * will be cancelled.
     *
     * @param plan         workflow plan
     * @param initialInput initial input
     * @param timeout      global timeout
     * @return Mono completing with execution context or error
     */
    public Mono<ReactiveExecutionContext> execute(WorkflowExecutionPlan plan, Object initialInput,
            java.time.Duration timeout) {
        return execute(plan, initialInput).timeout(timeout);
    }

    sealed protected interface Event permits TaskDone {
    }

    private record TaskDone(TaskNode node, TaskResult result) implements Event {
    }

    private Mono<Void> executeEventDriven(WorkflowInstance instance, Object initialInput) {
        return Mono.defer(() -> {
            if (instance.isFinished()) {
                return Mono.empty();
            }

            // Work queue: produced ONLY by the state loop (single producer)
            Sinks.Many<TaskNode> workSink = Sinks.many().multicast().onBackpressureBuffer();
            // State queue: produced by workers concurrently (multi producer)
            Sinks.Many<Event> stateSink = Sinks.many().unicast().onBackpressureBuffer();

            AtomicInteger inFlight = new AtomicInteger(0);
            AtomicBoolean terminated = new AtomicBoolean(false);
            Sinks.One<Void> done = Sinks.one();

            // Enqueue READY tasks, atomically transitioning READY -> RUNNING in the state
            // loop.
            Runnable emitReady = () -> {
                Set<TaskNode> ready = instance.readyTasks();
                for (TaskNode node : ready) {
                    if (instance.tryMarkRunning(node)) {
                        inFlight.incrementAndGet();
                        workSink.emitNext(node, RETRY_NON_SERIALIZED);
                    }
                }
            };

            // 1) State loop: single-writer for WorkflowInstance + scheduling new work
            Disposable stateSub = stateSink.asFlux()
                    .publishOn(stateScheduler)
                    .subscribe(ev -> {
                        if (terminated.get()) {
                            return;
                        }

                        if (ev instanceof TaskDone td) {
                            applyResult(instance, td.node(), td.result());

                            // Este task ya no está en vuelo
                            inFlight.decrementAndGet();

                            // Encola cualquier READY desbloqueado (esto puede aumentar inFlight)
                            emitReady.run();

                            // Dead-end detection: if no tasks ready, no tasks in flight, but workflow not
                            // finished
                            boolean noMoreReady = instance.readyTasks().isEmpty();
                            boolean noInFlight = inFlight.get() == 0;

                            if (noMoreReady && noInFlight && !instance.isFinished()) {
                                // Dead-end state - workflow cannot progress
                                Set<TaskId> pendingTasks = instance.plan().nodes().stream()
                                        .filter(node -> {
                                            TaskStatus status = instance.status(node);
                                            return status != TaskStatus.COMPLETED
                                                    && status != TaskStatus.FAILED
                                                    && status != TaskStatus.SKIPPED;
                                        })
                                        .map(TaskNode::id)
                                        .collect(java.util.stream.Collectors.toSet());

                                DeadEndException deadEnd = new DeadEndException(
                                        "Workflow reached dead-end state. No tasks ready, no tasks running, " +
                                                "but workflow not finished. Pending tasks: " + pendingTasks,
                                        pendingTasks);

                                if (terminated.compareAndSet(false, true)) {
                                    done.tryEmitError(deadEnd);
                                    workSink.tryEmitComplete();
                                    stateSink.tryEmitComplete();
                                }
                                return;
                            }

                            // Termina SOLO si no queda trabajo en vuelo ni tareas READY
                            if (instance.isFinished() || (noMoreReady && noInFlight)) {
                                if (terminated.compareAndSet(false, true)) {
                                    done.tryEmitEmpty();
                                    workSink.tryEmitComplete();
                                    stateSink.tryEmitComplete();
                                }
                            }
                        }
                    }, err -> {
                        if (terminated.compareAndSet(false, true)) {
                            done.tryEmitError(err);
                            workSink.tryEmitComplete();
                            stateSink.tryEmitComplete();
                        }
                    });

            // 2) Worker loop: execute tasks concurrently, publish completion events
            Disposable workSub = workSink.asFlux()
                    .flatMap(node -> {
                        // Record start time for duration tracking
                        taskStartTimes.put(node.id(), System.nanoTime());

                        // Track max in-flight
                        int currentInFlight = inFlight.get();
                        maxInFlightObserved.updateAndGet(max -> Math.max(max, currentInFlight));

                        monitor.onTaskStart(instance, node.id());

                        // Strict TaskResult contract enforcement:
                        // 1. Mono.defer() catches sync exceptions during task execution
                        // 2. .switchIfEmpty() after executeWithResult handles tasks returning
                        // Mono.empty()
                        // 3. .onErrorResume() converts any error to TaskResult.Failure
                        return Mono.defer(() -> inputResolver.resolveInput(instance, node, initialInput)
                                .defaultIfEmpty(NULL_SENTINEL)
                                .flatMap(input -> {
                                    Object actualInput = (input == NULL_SENTINEL) ? null : input;
                                    return node.executeWithResult(actualInput, instance.context());
                                }))
                                .switchIfEmpty(Mono.just(new TaskResult.Failure(
                                        new IllegalStateException("Task produced no result: " + node.id()))))

                                .onErrorResume(err -> Mono.just(new TaskResult.Failure(err)))
                                .subscribeOn(taskScheduler)
                                .doOnNext(
                                        result -> stateSink.emitNext(new TaskDone(node, result), RETRY_NON_SERIALIZED))
                                .then();
                    }, maxConcurrency)
                    .subscribe(
                            ignored -> {
                            },
                            err -> {
                                // If worker stream fails unexpectedly, terminate the workflow
                                if (terminated.compareAndSet(false, true)) {
                                    done.tryEmitError(err);
                                    workSink.tryEmitComplete();
                                    stateSink.tryEmitComplete();
                                }
                            });

            // Kickstart from the state loop thread
            stateScheduler.schedule(() -> {
                emitReady.run();

                // If there's no initial work and no in-flight, we're in a dead-end (or empty
                // plan)
                if (!terminated.get() && instance.readyTasks().isEmpty() && inFlight.get() == 0) {
                    terminated.set(true);
                    done.tryEmitEmpty();
                    workSink.tryEmitComplete();
                    stateSink.tryEmitComplete();
                }
            });

            return done.asMono()
                    .doFinally(sig -> {
                        workSub.dispose();
                        stateSub.dispose();
                    });
        });
    }

    private void applyResult(WorkflowInstance instance, TaskNode node, TaskResult result) {
        // Calculate duration
        Long startNanos = taskStartTimes.remove(node.id());
        java.time.Duration duration = java.time.Duration.ZERO;
        if (startNanos != null) {
            long endNanos = System.nanoTime();
            duration = java.time.Duration.ofNanos(endNanos - startNanos);
            taskDurations.put(node.id(), duration);
        }

        if (result instanceof TaskResult.Success success) {
            // Persist output for downstream consumption and for tests.
            instance.context().put(node.id(), success.output());
            instance.markCompleted(node);
            monitor.onTaskSuccess(instance, node.id(), duration);
            return;
        }
        if (result instanceof TaskResult.Skipped) {
            instance.markSkipped(node);
            monitor.onTaskSkipped(instance, node.id(), duration);
            return;
        }
        if (result instanceof TaskResult.Failure failure) {
            taskErrors.put(node.id(), failure.error());
            instance.markFailed(node);
            monitor.onTaskFailure(instance, node.id(), failure.error(), duration);
            return;
        }

        IllegalStateException unknownError = new IllegalStateException("Unknown TaskResult: " + result);
        taskErrors.put(node.id(), unknownError);
        instance.markFailed(node);
        monitor.onTaskFailure(instance, node.id(), unknownError, duration);
    }

    private io.tugrandsolutions.flowforge.workflow.report.ExecutionReport buildExecutionReport(
            WorkflowInstance instance) {
        java.util.Map<TaskId, TaskStatus> finalStatuses = new java.util.HashMap<>();
        int completed = 0, failed = 0, skipped = 0;

        for (TaskNode node : instance.plan().nodes()) {
            TaskStatus status = instance.status(node);
            finalStatuses.put(node.id(), status);

            if (status == TaskStatus.COMPLETED)
                completed++;
            else if (status == TaskStatus.FAILED)
                failed++;
            else if (status == TaskStatus.SKIPPED)
                skipped++;
        }

        return new io.tugrandsolutions.flowforge.workflow.report.ExecutionReport(
                finalStatuses,
                new java.util.HashMap<>(taskDurations),
                new java.util.HashMap<>(taskErrors),
                instance.plan().nodes().size(),
                completed,
                failed,
                skipped,
                maxInFlightObserved.get());
    }
}
