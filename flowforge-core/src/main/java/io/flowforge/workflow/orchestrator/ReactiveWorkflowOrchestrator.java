package io.flowforge.workflow.orchestrator;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.flowforge.task.TaskId;
import io.flowforge.task.TaskResult;
import io.flowforge.workflow.InMemoryReactiveExecutionContext;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.input.DefaultTaskInputResolver;
import io.flowforge.workflow.input.TaskInputResolver;
import io.flowforge.workflow.instance.ExecutionSession;
import io.flowforge.workflow.instance.TaskStatus;
import io.flowforge.workflow.instance.WorkflowInstance;
import io.flowforge.workflow.instance.WorkflowRunMetadata;
import io.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.flowforge.workflow.monitor.WorkflowMonitor;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.trace.*;
import io.flowforge.validation.TypeMetadata;
import io.flowforge.workflow.report.ExecutionReport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Production-hardened event-driven orchestrator.
 * Stateless and resource-bounded.
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
    private final ExecutionLimits limits;

    public ReactiveWorkflowOrchestrator() {
        this(Schedulers.boundedElastic(), new NoOpWorkflowMonitor(), new DefaultTaskInputResolver());
    }

    public ReactiveWorkflowOrchestrator(Scheduler taskScheduler, WorkflowMonitor monitor,
            TaskInputResolver inputResolver) {
        this(taskScheduler, Schedulers.newSingle("wf-state"), monitor, inputResolver,
                ExecutionLimits.defaultLimits());
    }

    public ReactiveWorkflowOrchestrator(Scheduler taskScheduler, Scheduler stateScheduler, WorkflowMonitor monitor,
            TaskInputResolver inputResolver, int maxConcurrency) {
        this(taskScheduler, stateScheduler, monitor, inputResolver,
             new ExecutionLimits(maxConcurrency, 1000, BackpressureStrategy.BLOCK));
    }

    public ReactiveWorkflowOrchestrator(Scheduler taskScheduler, Scheduler stateScheduler, WorkflowMonitor monitor,
            TaskInputResolver inputResolver, ExecutionLimits limits) {
        this(taskScheduler, stateScheduler, monitor, inputResolver, null, limits);
    }

    public ReactiveWorkflowOrchestrator(Scheduler taskScheduler, Scheduler stateScheduler, WorkflowMonitor monitor,
            TaskInputResolver inputResolver, ExecutionTracerFactory tracerFactory, ExecutionLimits limits) {
        this.taskScheduler = taskScheduler;
        this.stateScheduler = stateScheduler;
        this.monitor = monitor;
        this.inputResolver = inputResolver;
        this.tracerFactory = tracerFactory != null ? tracerFactory : (types -> new NoOpExecutionTracer());
        this.limits = limits;
    }

    public Mono<ReactiveExecutionContext> execute(String workflowId, WorkflowExecutionPlan plan, Object initialInput) {
        return execute(WorkflowRunMetadata.of(workflowId), plan, initialInput);
    }

    public Mono<ReactiveExecutionContext> execute(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            Object initialInput) {
        return execute(metadata, plan, initialInput, tracerFactory.create(plan.typeMetadata()));
    }

    public Mono<ReactiveExecutionContext> execute(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            Object initialInput, ExecutionTracer tracer) {
        
        ReactiveExecutionContext context = new InMemoryReactiveExecutionContext();
        WorkflowInstance instance = new WorkflowInstance(metadata, plan, context);
        ExecutionSession session = new ExecutionSession(instance, tracer);

        monitor.onWorkflowStart(instance);
        String executionId = metadata.correlationId() != null ? metadata.correlationId()
                : java.util.UUID.randomUUID().toString();
        tracer.onWorkflowStart(metadata.workflowId(), executionId);

        return executeEventDriven(session, initialInput)
                .doOnSuccess(ignored -> tracer.onWorkflowSuccess())
                .doOnError(tracer::onWorkflowError)
                .doOnCancel(tracer::onWorkflowCanceled)
                .materialize()
                .doOnNext(signal -> {
                    ExecutionReport report = buildExecutionReport(session);
                    monitor.onWorkflowComplete(instance, report);
                })
                .dematerialize()
                .doOnCancel(() -> {
                    ExecutionReport report = buildExecutionReport(session);
                    monitor.onWorkflowComplete(instance, report);
                })
                .thenReturn(context);
    }

    public Mono<ExecutionTrace> executeWithTrace(WorkflowExecutionPlan plan, Object initialInput,
            java.util.Map<String, TypeMetadata> typeMetadata) {
        ExecutionTracer internalTracer = new DefaultExecutionTracer(typeMetadata);
        ExecutionTracer configuredTracer = tracerFactory.create(typeMetadata);
        ExecutionTracer composite = new CompositeExecutionTracer(List.of(internalTracer, configuredTracer));

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

    private Mono<Void> executeEventDriven(ExecutionSession session, Object initialInput) {
        return Mono.defer(() -> {
            WorkflowInstance instance = session.instance();
            if (instance.isFinished()) {
                return Mono.empty();
            }

            // Task emission sink - bounded by limits.maxQueueSize
            Sinks.Many<TaskNode> workSink = Sinks.many().multicast().onBackpressureBuffer(
                    limits.maxQueueSize(), false);
            
            // Internal state events
            Sinks.Many<Event> stateSink = Sinks.many().unicast().onBackpressureBuffer();

            AtomicBoolean terminated = new AtomicBoolean(false);
            Sinks.One<Void> done = Sinks.one();

            Runnable emitReady = () -> {
                // DETERMINISM: Sort tasks by ID to ensure stable execution order for parallel branches
                List<TaskNode> ready = instance.readyTasks().stream()
                        .sorted(Comparator.comparing(t -> t.id().getValue()))
                        .toList();
                
                for (TaskNode node : ready) {
                    if (instance.tryMarkRunning(node)) {
                        Sinks.EmitResult result = workSink.tryEmitNext(node);
                        if (result.isFailure()) {
                            handleBackpressureFailure(result, done, terminated, workSink, stateSink);
                            return;
                        }
                    }
                }
            };

            Disposable stateSub = stateSink.asFlux()
                    .publishOn(stateScheduler)
                    .subscribe(ev -> {
                        if (terminated.get()) return;

                        if (ev instanceof TaskDone td) {
                            applyResult(session, td.node(), td.result());
                            emitReady.run();

                            if (instance.isFinished()) {
                                completeWorkflow(session, done, terminated, workSink, stateSink);
                            } else if (instance.readyTasks().isEmpty() && session.inFlightCount() == 0) {
                                completeWorkflow(session, done, terminated, workSink, stateSink);
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
                        session.recordTaskStart(node.id());
                        monitor.onTaskStart(session.instance(), node.id());
                        
                        java.util.List<String> depIds = node.dependencies().stream()
                                .map(d -> d.id().getValue())
                                .toList();
                        session.tracer().onTaskStart(node.id().getValue(), depIds);

                        return Mono.defer(() -> inputResolver.resolveInput(session.instance(), node, initialInput)
                                .defaultIfEmpty(NULL_SENTINEL)
                                .flatMap(input -> {
                                    Object actualInput = (input == NULL_SENTINEL) ? null : input;
                                    return node.executeWithResult(actualInput, session.instance().context());
                                }))
                                .switchIfEmpty(Mono.just(new TaskResult.Failure(
                                        new IllegalStateException("Task produced no result: " + node.id()))))
                                .onErrorResume(err -> Mono.just(new TaskResult.Failure(err)))
                                .subscribeOn(taskScheduler)
                                .doOnNext(result -> stateSink.emitNext(new TaskDone(node, result), RETRY_NON_SERIALIZED))
                                .then();
                    }, limits.maxInFlightTasks()) // Concurrency limited globally per workflow
                    .subscribe(ignored -> {}, err -> {
                        if (terminated.compareAndSet(false, true)) {
                            done.tryEmitError(err);
                            workSink.tryEmitComplete();
                            stateSink.tryEmitComplete();
                        }
                    });

            stateScheduler.schedule(() -> {
                emitReady.run();
                if (instance.isFinished() || (instance.readyTasks().isEmpty() && session.inFlightCount() == 0)) {
                    completeWorkflow(session, done, terminated, workSink, stateSink);
                }
            });

            return done.asMono().doFinally(sig -> {
                workSub.dispose();
                stateSub.dispose();
            });
        });
    }

    private void handleBackpressureFailure(Sinks.EmitResult result, Sinks.One<Void> done, AtomicBoolean terminated, 
                                          Sinks.Many<?> work, Sinks.Many<?> state) {
        if (terminated.compareAndSet(false, true)) {
            String msg = "Workflow backpressure limit exceeded: " + result;
            done.tryEmitError(new IllegalStateException(msg));
            work.tryEmitComplete();
            state.tryEmitComplete();
        }
    }

    private void completeWorkflow(ExecutionSession session, Sinks.One<Void> done, AtomicBoolean terminated, 
                                 Sinks.Many<?> work, Sinks.Many<?> state) {
        if (!terminated.compareAndSet(false, true)) return;

        WorkflowInstance instance = session.instance();
        // Check for fatal errors (non-optional tasks that failed)
        var fatalError = session.taskErrors().entrySet().stream()
                .filter(e -> instance.plan().getNode(e.getKey())
                        .map(n -> !n.descriptor().optional() && instance.status(n) == TaskStatus.FAILED)
                        .orElse(false))
                .map(java.util.Map.Entry::getValue)
                .findFirst();

        if (fatalError.isPresent()) {
            done.tryEmitError(fatalError.get());
        } else if (!instance.isFinished()) {
            // Dead-end
            Set<TaskId> pending = instance.plan().nodes().stream()
                    .filter(node -> {
                        TaskStatus status = instance.status(node);
                        return status != TaskStatus.COMPLETED && status != TaskStatus.FAILED && status != TaskStatus.SKIPPED;
                    })
                    .map(TaskNode::id)
                    .collect(Collectors.toSet());
            done.tryEmitError(new DeadEndException("Dead-end detected", pending));
        } else {
            done.tryEmitEmpty();
        }
        
        work.tryEmitComplete();
        state.tryEmitComplete();
    }

    private void applyResult(ExecutionSession session, TaskNode node, TaskResult result) {
        WorkflowInstance instance = session.instance();
        java.time.Duration duration = session.recordTaskCompletion(node.id());

        if (result instanceof TaskResult.Success success) {
            instance.context().put(node.id(), success.output());
            instance.markCompleted(node);
            monitor.onTaskSuccess(instance, node.id(), duration);
            session.tracer().onTaskSuccess(node.id().getValue(), success.output());
        } else if (result instanceof TaskResult.Skipped) {
            instance.markSkipped(node);
            monitor.onTaskSkipped(instance, node.id(), duration);
            session.tracer().onTaskSkipped(node.id().getValue());
        } else if (result instanceof TaskResult.Failure failure) {
            session.recordTaskError(node.id(), failure.error());
            instance.markFailed(node);
            monitor.onTaskFailure(instance, node.id(), failure.error(), duration);
            session.tracer().onTaskError(node.id().getValue(), failure.error());
        }
    }

    private ExecutionReport buildExecutionReport(ExecutionSession session) {
        WorkflowInstance instance = session.instance();
        java.util.Map<TaskId, TaskStatus> finalStatuses = new java.util.HashMap<>();
        int completed = 0, failed = 0, skipped = 0;

        for (TaskNode node : instance.plan().nodes()) {
            TaskStatus status = instance.status(node);
            finalStatuses.put(node.id(), status);
            if (status == TaskStatus.COMPLETED) completed++;
            else if (status == TaskStatus.FAILED) failed++;
            else if (status == TaskStatus.SKIPPED) skipped++;
        }

        return new ExecutionReport(
            finalStatuses, 
            new java.util.HashMap<>(session.taskDurations()),
            new java.util.HashMap<>(session.taskErrors()), 
            instance.plan().nodes().size(),
            completed, failed, skipped, 
            session.maxInFlightObserved()
        );
    }

    protected sealed interface Event permits TaskDone {}
    private record TaskDone(TaskNode node, TaskResult result) implements Event {}
}
