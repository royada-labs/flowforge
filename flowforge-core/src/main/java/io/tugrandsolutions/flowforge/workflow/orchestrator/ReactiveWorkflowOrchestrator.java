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
import io.tugrandsolutions.flowforge.workflow.trace.*;
import io.tugrandsolutions.flowforge.validation.TypeMetadata;
import io.tugrandsolutions.flowforge.workflow.report.ExecutionReport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Fully event-driven orchestrator.
 */
public final class ReactiveWorkflowOrchestrator {

    private static final Sinks.EmitFailureHandler RETRY_NON_SERIALIZED = (signalType,
            emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED;

    private static final Object NULL_SENTINEL = new Object();

    private final Scheduler taskScheduler;
    private final Scheduler stateScheduler;
    private final WorkflowMonitor monitor;
    private final TaskInputResolver inputResolver;
    private final ExecutionTracerFactory tracerFactory;
    private final int maxConcurrency;

    // Timing tracking for observability
    private final java.util.concurrent.ConcurrentHashMap<TaskId, Long> taskStartTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<TaskId, java.time.Duration> taskDurations = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<TaskId, Throwable> taskErrors = new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicInteger maxInFlightObserved = new AtomicInteger(0);

    public ReactiveWorkflowOrchestrator() {
        this(Schedulers.boundedElastic(), new NoOpWorkflowMonitor(), new DefaultTaskInputResolver());
    }

    public ReactiveWorkflowOrchestrator(Scheduler taskScheduler, WorkflowMonitor monitor,
            TaskInputResolver inputResolver) {
        this(taskScheduler, Schedulers.newSingle("wf-state"), monitor, inputResolver,
                Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public ReactiveWorkflowOrchestrator(Scheduler taskScheduler, Scheduler stateScheduler, WorkflowMonitor monitor,
            TaskInputResolver inputResolver, int maxConcurrency) {
        this(taskScheduler, stateScheduler, monitor, inputResolver, null, maxConcurrency);
    }

    public ReactiveWorkflowOrchestrator(Scheduler taskScheduler, Scheduler stateScheduler, WorkflowMonitor monitor,
            TaskInputResolver inputResolver, ExecutionTracerFactory tracerFactory, int maxConcurrency) {
        this.taskScheduler = taskScheduler;
        this.stateScheduler = stateScheduler;
        this.monitor = monitor;
        this.inputResolver = inputResolver;
        this.tracerFactory = tracerFactory != null ? tracerFactory : (types -> new NoOpExecutionTracer());
        this.maxConcurrency = maxConcurrency;
    }

    public Mono<ReactiveExecutionContext> execute(String workflowId, WorkflowExecutionPlan plan, Object initialInput) {
        return execute(WorkflowRunMetadata.of(workflowId), plan, initialInput);
    }

    public Mono<ReactiveExecutionContext> execute(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            Object initialInput) {
        // Automatically create tracers from configured factory (e.g. OTel)
        return execute(metadata, plan, initialInput, tracerFactory.create(plan.typeMetadata()));
    }

    public Mono<ReactiveExecutionContext> execute(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            Object initialInput, ExecutionTracer tracer) {
        ReactiveExecutionContext context = new InMemoryReactiveExecutionContext();
        WorkflowInstance instance = new WorkflowInstance(metadata, plan, context);

        monitor.onWorkflowStart(instance);
        String executionId = metadata.correlationId() != null ? metadata.correlationId()
                : java.util.UUID.randomUUID().toString();
        tracer.onWorkflowStart(metadata.workflowId(), executionId);

        return executeEventDriven(instance, initialInput, tracer)
                .doOnSuccess(ignored -> tracer.onWorkflowSuccess())
                .doOnError(tracer::onWorkflowError)
                .doOnCancel(tracer::onWorkflowCanceled)
                .materialize()
                .doOnNext(signal -> {
                    ExecutionReport report = buildExecutionReport(instance);
                    monitor.onWorkflowComplete(instance, report);
                })
                .dematerialize()
                .doOnCancel(() -> {
                    ExecutionReport report = buildExecutionReport(instance);
                    monitor.onWorkflowComplete(instance, report);
                })
                .thenReturn(context);
    }

    public Mono<ExecutionTrace> executeWithTrace(WorkflowExecutionPlan plan, Object initialInput,
            java.util.Map<String, TypeMetadata> typeMetadata) {
        // Internal timeline tracer
        ExecutionTracer internalTracer = new DefaultExecutionTracer(typeMetadata);

        // Other configured tracers (e.g. OTel)
        ExecutionTracer configuredTracer = tracerFactory.create(typeMetadata);

        // Composite them together
        ExecutionTracer composite = new CompositeExecutionTracer(
                java.util.List.of(internalTracer, configuredTracer));

        return execute(WorkflowRunMetadata.of("TRACE-RUN-" + java.util.UUID.randomUUID()), plan, initialInput, composite)
                .then(Mono.fromCallable(composite::build));
    }

    @Deprecated
    public Mono<ReactiveExecutionContext> execute(WorkflowExecutionPlan plan, Object initialInput) {
        return execute("UNKNOWN", plan, initialInput);
    }

    public Mono<ReactiveExecutionContext> execute(WorkflowExecutionPlan plan, Object initialInput,
            java.time.Duration timeout) {
        return execute(plan, initialInput).timeout(timeout);
    }

    sealed protected interface Event permits TaskDone {
    }

    private record TaskDone(TaskNode node, TaskResult result) implements Event {
    }

    private Mono<Void> executeEventDriven(WorkflowInstance instance, Object initialInput, ExecutionTracer tracer) {
        return Mono.defer(() -> {
            if (instance.isFinished()) {
                return Mono.empty();
            }

            Sinks.Many<TaskNode> workSink = Sinks.many().multicast().onBackpressureBuffer();
            Sinks.Many<Event> stateSink = Sinks.many().unicast().onBackpressureBuffer();

            AtomicInteger inFlight = new AtomicInteger(0);
            AtomicBoolean terminated = new AtomicBoolean(false);
            Sinks.One<Void> done = Sinks.one();

            Runnable emitReady = () -> {
                Set<TaskNode> ready = instance.readyTasks();
                for (TaskNode node : ready) {
                    if (instance.tryMarkRunning(node)) {
                        inFlight.incrementAndGet();
                        workSink.emitNext(node, RETRY_NON_SERIALIZED);
                    }
                }
            };

            Disposable stateSub = stateSink.asFlux()
                    .publishOn(stateScheduler)
                    .subscribe(ev -> {
                        if (terminated.get())
                            return;

                        if (ev instanceof TaskDone td) {
                            applyResult(instance, td.node(), td.result(), tracer);
                            inFlight.decrementAndGet();
                            emitReady.run();

                            boolean noMoreReady = instance.readyTasks().isEmpty();
                            boolean noInFlight = inFlight.get() == 0;

                            if (noMoreReady && noInFlight && !instance.isFinished()) {
                                java.util.Optional<Throwable> fatalError = taskErrors.entrySet().stream()
                                        .filter(e -> instance.plan().getNode(e.getKey())
                                                .map(n -> instance.status(n) == TaskStatus.FAILED)
                                                .orElse(false))
                                        .map(java.util.Map.Entry::getValue)
                                        .findFirst();

                                if (fatalError.isPresent()) {
                                    if (terminated.compareAndSet(false, true)) {
                                        done.tryEmitError(fatalError.get());
                                        workSink.tryEmitComplete();
                                        stateSink.tryEmitComplete();
                                    }
                                    return;
                                }

                                Set<TaskId> pendingTasks = instance.plan().nodes().stream()
                                        .filter(node -> {
                                            TaskStatus status = instance.status(node);
                                            return status != TaskStatus.COMPLETED && status != TaskStatus.FAILED
                                                    && status != TaskStatus.SKIPPED;
                                        })
                                        .map(TaskNode::id)
                                        .collect(java.util.stream.Collectors.toSet());

                                DeadEndException deadEnd = new DeadEndException(
                                        "Dead-end: " + pendingTasks,
                                        pendingTasks);

                                if (terminated.compareAndSet(false, true)) {
                                    done.tryEmitError(deadEnd);
                                    workSink.tryEmitComplete();
                                    stateSink.tryEmitComplete();
                                }
                                return;
                            }

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

            Disposable workSub = workSink.asFlux()
                    .flatMap(node -> {
                        taskStartTimes.put(node.id(), System.nanoTime());
                        maxInFlightObserved.updateAndGet(max -> Math.max(max, inFlight.get()));

                        monitor.onTaskStart(instance, node.id());
                        tracer.onTaskStart(node.id().getValue());

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
                                .doOnNext(result -> stateSink.emitNext(new TaskDone(node, result), RETRY_NON_SERIALIZED))
                                .then();
                    }, maxConcurrency)
                    .subscribe(ignored -> {
                    }, err -> {
                        if (terminated.compareAndSet(false, true)) {
                            done.tryEmitError(err);
                            workSink.tryEmitComplete();
                            stateSink.tryEmitComplete();
                        }
                    });

            stateScheduler.schedule(() -> {
                emitReady.run();
                if (!terminated.get() && instance.readyTasks().isEmpty() && inFlight.get() == 0) {
                    terminated.set(true);
                    done.tryEmitEmpty();
                    workSink.tryEmitComplete();
                    stateSink.tryEmitComplete();
                }
            });

            return done.asMono().doFinally(sig -> {
                workSub.dispose();
                stateSub.dispose();
            });
        });
    }

    private void applyResult(WorkflowInstance instance, TaskNode node, TaskResult result, ExecutionTracer tracer) {
        Long startNanos = taskStartTimes.remove(node.id());
        java.time.Duration duration = java.time.Duration.ZERO;
        if (startNanos != null) {
            duration = java.time.Duration.ofNanos(System.nanoTime() - startNanos);
            taskDurations.put(node.id(), duration);
        }

        if (result instanceof TaskResult.Success success) {
            instance.context().put(node.id(), success.output());
            instance.markCompleted(node);
            monitor.onTaskSuccess(instance, node.id(), duration);
            tracer.onTaskSuccess(node.id().getValue(), success.output());
            return;
        }
        if (result instanceof TaskResult.Skipped) {
            instance.markSkipped(node);
            monitor.onTaskSkipped(instance, node.id(), duration);
            tracer.onTaskSkipped(node.id().getValue());
            return;
        }
        if (result instanceof TaskResult.Failure failure) {
            taskErrors.put(node.id(), failure.error());
            instance.markFailed(node);
            monitor.onTaskFailure(instance, node.id(), failure.error(), duration);
            tracer.onTaskError(node.id().getValue(), failure.error());
            return;
        }

        IllegalStateException unknownError = new IllegalStateException("Unknown TaskResult: " + result);
        taskErrors.put(node.id(), unknownError);
        instance.markFailed(node);
        monitor.onTaskFailure(instance, node.id(), unknownError, duration);
    }

    private ExecutionReport buildExecutionReport(WorkflowInstance instance) {
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

        return new ExecutionReport(finalStatuses, new java.util.HashMap<>(taskDurations),
                new java.util.HashMap<>(taskErrors), instance.plan().nodes().size(),
                completed, failed, skipped, maxInFlightObserved.get());
    }
}
