package org.royada.flowforge.workflow.orchestrator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.royada.flowforge.exception.DeadEndException;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.task.TaskResult;
import org.royada.flowforge.validation.TypeMetadata;
import org.royada.flowforge.workflow.InMemoryReactiveExecutionContext;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.input.DefaultTaskInputResolver;
import org.royada.flowforge.workflow.input.TaskInputResolver;
import org.royada.flowforge.workflow.instance.ExecutionSession;
import org.royada.flowforge.workflow.instance.TaskStatus;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
import org.royada.flowforge.workflow.instance.WorkflowRunMetadata;
import org.royada.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import org.royada.flowforge.workflow.monitor.WorkflowMonitor;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.report.ExecutionReport;
import org.royada.flowforge.workflow.trace.CompositeExecutionTracer;
import org.royada.flowforge.workflow.trace.DefaultExecutionTracer;
import org.royada.flowforge.workflow.trace.ExecutionTrace;
import org.royada.flowforge.workflow.trace.ExecutionTracer;
import org.royada.flowforge.workflow.trace.ExecutionTracerFactory;
import org.royada.flowforge.workflow.trace.NoOpExecutionTracer;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Production-hardened event-driven orchestrator.
 * Stateless and resource-bounded.
 * 
 * <p>Use {@link #builder()} to create instances with custom configuration.
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

    /**
     * Creates a new {@link Builder} to configure and create an orchestrator instance.
     * 
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private ReactiveWorkflowOrchestrator(Builder builder) {
        this.taskScheduler = builder.taskScheduler;
        this.stateScheduler = builder.stateScheduler;
        this.monitor = builder.monitor;
        this.inputResolver = builder.inputResolver;
        this.tracerFactory = builder.tracerFactory;
        this.limits = builder.limits;
    }

    /**
     * Executes a workflow plan with the given initial input.
     * 
     * @param workflowId the ID of the workflow to execute
     * @param plan the execution plan
     * @param initialInput the initial input for the workflow
     * @return a Mono that completes with the final execution context
     */
    public Mono<ReactiveExecutionContext> execute(String workflowId, WorkflowExecutionPlan plan, Object initialInput) {
        return execute(WorkflowRunMetadata.of(workflowId), plan, initialInput);
    }

    /**
     * Executes a workflow plan with the given metadata and initial input.
     * 
     * @param metadata the run metadata
     * @param plan the execution plan
     * @param initialInput the initial input for the workflow
     * @return a Mono that completes with the final execution context
     */
    public Mono<ReactiveExecutionContext> execute(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            Object initialInput) {
        return execute(metadata, plan, initialInput, tracerFactory.create(plan.typeMetadata()));
    }

    /**
     * Executes a workflow plan with the given metadata, initial input, and custom tracer.
     * 
     * @param metadata the run metadata
     * @param plan the execution plan
     * @param initialInput the initial input for the workflow
     * @param tracer the tracer to use for this execution
     * @return a Mono that completes with the final execution context
     */
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

    /**
     * Executes a workflow plan and returns an execution trace.
     * 
     * @param plan the execution plan
     * @param initialInput the initial input
     * @param typeMetadata the type metadata for the tasks
     * @return a Mono that completes with the execution trace
     */
    public Mono<ExecutionTrace> executeWithTrace(WorkflowExecutionPlan plan, Object initialInput,
            Map<TaskId, TypeMetadata> typeMetadata) {
        ExecutionTracer internalTracer = new DefaultExecutionTracer(typeMetadata);
        ExecutionTracer configuredTracer = tracerFactory.create(typeMetadata);
        ExecutionTracer composite = new CompositeExecutionTracer(List.of(internalTracer, configuredTracer));

        return execute(WorkflowRunMetadata.of("TRACE-RUN-" + java.util.UUID.randomUUID()), plan, initialInput, composite)
                .then(Mono.fromCallable(composite::build));
    }

    /**
     * Executes a workflow plan with default metadata.
     * 
     * @param plan the execution plan
     * @param initialInput the initial input
     * @return a Mono that completes with the final execution context
     */
    public Mono<ReactiveExecutionContext> execute(WorkflowExecutionPlan plan, Object initialInput) {
        return execute("UNKNOWN", plan, initialInput);
    }

    /**
     * Executes a workflow plan with a timeout.
     * 
     * @param plan the execution plan
     * @param initialInput the initial input
     * @param timeout the execution timeout
     * @return a Mono that completes with the final execution context or errors with a timeout
     */
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

            Sinks.Many<TaskNode> workSink = createWorkSink();
            Sinks.Many<Event> stateSink = createStateSink();
            AtomicBoolean terminated = new AtomicBoolean(false);
            Sinks.One<Void> done = Sinks.one();

            Disposable stateSub = createStateSubscription(session, done, terminated, workSink, stateSink);
            Disposable workSub = createWorkSubscription(session, initialInput, done, terminated, workSink, stateSink);
            
            scheduleInitialExecution(session, done, terminated, workSink, stateSink);

            return done.asMono().doFinally(sig -> {
                workSub.dispose();
                stateSub.dispose();
            });
        });
    }

    private Sinks.Many<TaskNode> createWorkSink() {
        return Sinks.many().multicast().onBackpressureBuffer(limits.maxQueueSize(), false);
    }

    private Sinks.Many<Event> createStateSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    private void emitReadyTasks(WorkflowInstance instance, Sinks.Many<TaskNode> workSink,
                               Sinks.One<Void> done, AtomicBoolean terminated, Sinks.Many<Event> stateSink) {
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
    }

    private Disposable createStateSubscription(ExecutionSession session, Sinks.One<Void> done,
                                              AtomicBoolean terminated, Sinks.Many<TaskNode> workSink,
                                              Sinks.Many<Event> stateSink) {
        return stateSink.asFlux()
                .publishOn(stateScheduler)
                .subscribe(ev -> {
                    if (terminated.get()) return;

                    if (ev instanceof TaskDone td) {
                        applyResult(session, td.node(), td.result());
                        emitReadyTasks(session.instance(), workSink, done, terminated, stateSink);

                        if (session.instance().isFinished()) {
                            completeWorkflow(session, done, terminated, workSink, stateSink);
                        } else if (session.instance().readyTasks().isEmpty() && session.inFlightCount() == 0) {
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
    }

    private Disposable createWorkSubscription(ExecutionSession session, Object initialInput,
                                            Sinks.One<Void> done, AtomicBoolean terminated,
                                            Sinks.Many<TaskNode> workSink, Sinks.Many<Event> stateSink) {
        return workSink.asFlux()
                .flatMap(node -> executeTaskNode(session, node, initialInput, stateSink),
                        limits.maxInFlightTasks())
                .subscribe(ignored -> {}, err -> {
                    if (terminated.compareAndSet(false, true)) {
                        done.tryEmitError(err);
                        workSink.tryEmitComplete();
                        stateSink.tryEmitComplete();
                    }
                });
    }

    private Mono<Void> executeTaskNode(ExecutionSession session, TaskNode node, Object initialInput,
                                      Sinks.Many<Event> stateSink) {
        session.recordTaskStart(node.id());
        monitor.onTaskStart(session.instance(), node.id());
        
        List<TaskId> depIds = node.dependencies().stream()
                .map(TaskNode::id)
                .toList();
        session.tracer().onTaskStart(node.id(), depIds);

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
    }

    private void scheduleInitialExecution(ExecutionSession session, Sinks.One<Void> done,
                                          AtomicBoolean terminated, Sinks.Many<TaskNode> workSink,
                                          Sinks.Many<Event> stateSink) {
        WorkflowInstance instance = session.instance();
        stateScheduler.schedule(() -> {
            emitReadyTasks(instance, workSink, done, terminated, stateSink);
            if (instance.isFinished() || (instance.readyTasks().isEmpty() && session.inFlightCount() == 0)) {
                completeWorkflow(session, done, terminated, workSink, stateSink);
            }
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
        var fatalError = session.taskErrors().entrySet().stream()
                .filter(e -> instance.plan().getNode(e.getKey())
                        .map(n -> !n.descriptor().optional() && instance.status(n) == TaskStatus.FAILED)
                        .orElse(false))
                .map(Map.Entry::getValue)
                .findFirst();

        if (fatalError.isPresent()) {
            done.tryEmitError(fatalError.get());
        } else if (!instance.isFinished()) {
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
            putTaskOutput(instance.context(), node.descriptor().task(), success.output());

            instance.markCompleted(node);
            monitor.onTaskSuccess(instance, node.id(), duration);
            session.tracer().onTaskSuccess(node.id(), success.output());
        } else if (result instanceof TaskResult.Skipped) {
            instance.markSkipped(node);
            monitor.onTaskSkipped(instance, node.id(), duration);
            session.tracer().onTaskSkipped(node.id());
        } else if (result instanceof TaskResult.Failure failure) {
            session.recordTaskError(node.id(), failure.error());
            instance.markFailed(node);
            monitor.onTaskFailure(instance, node.id(), failure.error(), duration);
            session.tracer().onTaskError(node.id(), failure.error());
        }
    }

    private static <O> void putTaskOutput(
            ReactiveExecutionContext context,
            Task<?, O> task,
            Object output
    ) {
        O typedOutput = output == null ? null : task.outputType().cast(output);
        context.put(task.outputKey(), typedOutput);
    }

    private ExecutionReport buildExecutionReport(ExecutionSession session) {
        WorkflowInstance instance = session.instance();
        Map<TaskId, TaskStatus> finalStatuses = new java.util.HashMap<>();
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

    /** Internal event for coordination. */
    protected sealed interface Event permits TaskDone {}
    /** Event for task completion. */
    private record TaskDone(TaskNode node, TaskResult result) implements Event {}

    /**
     * Builder for {@link ReactiveWorkflowOrchestrator}.
     */
    public static final class Builder {
        private Scheduler taskScheduler = Schedulers.boundedElastic();
        private Scheduler stateScheduler = Schedulers.newSingle("wf-state");
        private WorkflowMonitor monitor = new NoOpWorkflowMonitor();
        private TaskInputResolver inputResolver = new DefaultTaskInputResolver();
        private ExecutionTracerFactory tracerFactory = types -> new NoOpExecutionTracer();
        private ExecutionLimits limits = ExecutionLimits.defaultLimits();

        private Builder() {}

        /**
         * Sets the scheduler used for task execution.
         * 
         * @param taskScheduler the task scheduler
         * @return this builder
         */
        public Builder taskScheduler(Scheduler taskScheduler) {
            this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler");
            return this;
        }

        /**
         * Sets the scheduler used for state management.
         * 
         * @param stateScheduler the state scheduler
         * @return this builder
         */
        public Builder stateScheduler(Scheduler stateScheduler) {
            this.stateScheduler = Objects.requireNonNull(stateScheduler, "stateScheduler");
            return this;
        }

        /**
         * Sets the workflow monitor.
         * 
         * @param monitor the monitor
         * @return this builder
         */
        public Builder monitor(WorkflowMonitor monitor) {
            this.monitor = Objects.requireNonNull(monitor, "monitor");
            return this;
        }

        /**
         * Sets the task input resolver.
         * 
         * @param inputResolver the input resolver
         * @return this builder
         */
        public Builder inputResolver(TaskInputResolver inputResolver) {
            this.inputResolver = Objects.requireNonNull(inputResolver, "inputResolver");
            return this;
        }

        /**
         * Sets the execution tracer factory.
         * 
         * @param tracerFactory the tracer factory
         * @return this builder
         */
        public Builder tracerFactory(ExecutionTracerFactory tracerFactory) {
            this.tracerFactory = Objects.requireNonNull(tracerFactory, "tracerFactory");
            return this;
        }

        /**
         * Sets the execution limits.
         * 
         * @param limits the limits
         * @return this builder
         */
        public Builder limits(ExecutionLimits limits) {
            this.limits = Objects.requireNonNull(limits, "limits");
            return this;
        }

        /**
         * Sets the maximum concurrency for task execution.
         * 
         * @param maxConcurrency the maximum concurrency
         * @return this builder
         */
        public Builder maxConcurrency(int maxConcurrency) {
            this.limits = new ExecutionLimits(maxConcurrency, 1000, BackpressureStrategy.BLOCK);
            return this;
        }

        /**
         * Builds the {@link ReactiveWorkflowOrchestrator} instance.
         * 
         * @return the orchestrator
         */
        public ReactiveWorkflowOrchestrator build() {
            return new ReactiveWorkflowOrchestrator(this);
        }
    }
}
