package io.tugrandsolutions.flowforge.workflow.orchestrator;

import io.tugrandsolutions.flowforge.task.TaskResult;
import io.tugrandsolutions.flowforge.workflow.InMemoryReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.input.DefaultTaskInputResolver;
import io.tugrandsolutions.flowforge.workflow.input.TaskInputResolver;
import io.tugrandsolutions.flowforge.workflow.instance.WorkflowInstance;
import io.tugrandsolutions.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.monitor.WorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Set;

public final class ReactiveWorkflowOrchestrator {

    private final Scheduler scheduler;
    private final WorkflowMonitor monitor;
    private final TaskInputResolver inputResolver;

    public ReactiveWorkflowOrchestrator() {
        this(Schedulers.boundedElastic(), new NoOpWorkflowMonitor(), new DefaultTaskInputResolver());
    }

    public ReactiveWorkflowOrchestrator(Scheduler scheduler, WorkflowMonitor monitor) {
        this(scheduler, monitor, new DefaultTaskInputResolver());
    }

    public ReactiveWorkflowOrchestrator(
            Scheduler scheduler,
            WorkflowMonitor monitor,
            TaskInputResolver inputResolver
    ) {
        this.scheduler = scheduler;
        this.monitor = monitor;
        this.inputResolver = inputResolver;
    }

    public Mono<ReactiveExecutionContext> execute(
            WorkflowExecutionPlan plan,
            Object initialInput
    ) {
        ReactiveExecutionContext context =
                new InMemoryReactiveExecutionContext();

        WorkflowInstance instance =
                new WorkflowInstance(plan, context);
        monitor.onWorkflowStart(instance);
        return executeInstance(instance, initialInput)
                .doFinally(sig -> monitor.onWorkflowComplete(instance))
                .thenReturn(context);
    }

    private Mono<Void> executeInstance(
            WorkflowInstance instance,
            Object initialInput
    ) {
        return Mono.defer(() -> {
            if (instance.isFinished()) {
                return Mono.empty();
            }

            Set<TaskNode> ready = instance.readyTasks();

            if (ready.isEmpty()) {
                return Mono.empty();
            }

            return Flux.fromIterable(ready)
                    .flatMap(node -> executeNode(node, instance, initialInput))
                    .then()
                    .then(executeInstance(instance, initialInput));
        });
    }

    private Mono<Void> executeNode(
            TaskNode node,
            WorkflowInstance instance,
            Object initialInput
    ) {
        if (!instance.tryMarkRunning(node)) {
            return Mono.empty();
        }

        monitor.onTaskStart(instance, node.id());

        return inputResolver.resolveInput(instance, node, initialInput)
                .flatMap(resolvedInput -> node.executeWithResult(resolvedInput, instance.context()))
                .subscribeOn(scheduler)
                .doOnNext(result -> {
                    if (result instanceof TaskResult.Success success) {
                        instance.markCompleted(node);
                        monitor.onTaskSuccess(instance, node.id());

                    } else if (result instanceof TaskResult.Skipped) {
                        instance.markSkipped(node);
                        monitor.onTaskSkipped(instance, node.id());

                    } else if (result instanceof TaskResult.Failure failure) {
                        instance.markFailed(node);
                        monitor.onTaskFailure(instance, node.id(), failure.error());
                    }
                })
                .then();
    }
}
