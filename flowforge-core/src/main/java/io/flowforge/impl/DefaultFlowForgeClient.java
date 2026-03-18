package io.flowforge.impl;

import java.time.Duration;
import java.util.Objects;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.exception.UnknownWorkflowException;
import io.flowforge.exception.WorkflowExecutionException;
import io.flowforge.registry.WorkflowPlanRegistry;
import io.flowforge.validation.TypeMetadata;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.trace.ExecutionTrace;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DefaultFlowForgeClient implements FlowForgeClient {

    private final WorkflowPlanRegistry planRegistry;
    private final ReactiveWorkflowOrchestrator orchestrator;

    public DefaultFlowForgeClient(
            WorkflowPlanRegistry planRegistry,
            ReactiveWorkflowOrchestrator orchestrator) {
        this.planRegistry = Objects.requireNonNull(planRegistry, "planRegistry");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
    }

    @Override
    public Mono<ReactiveExecutionContext> execute(String workflowId, Object input) {
        WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                .orElseThrow(() -> new UnknownWorkflowException(workflowId));
        validateInitialInput(workflowId, plan, input);

        return orchestrator.execute(workflowId, plan, input)
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<ReactiveExecutionContext> execute(String workflowId, Object input, Duration timeout) {
        WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                .orElseThrow(() -> new UnknownWorkflowException(workflowId));
        validateInitialInput(workflowId, plan, input);

        return orchestrator.execute(workflowId, plan, input)
                .timeout(timeout)
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<Object> executeResult(String workflowId, Object input) {
        WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                .orElseThrow(() -> new UnknownWorkflowException(workflowId));
        validateInitialInput(workflowId, plan, input);

        return orchestrator.execute(workflowId, plan, input)
                .map(ctx -> io.flowforge.workflow.result.WorkflowResultSelector
                        .select(plan, ctx))
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<ExecutionTrace> executeWithTrace(String workflowId, Object input) {
        WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                .orElseThrow(() -> new UnknownWorkflowException(workflowId));
        validateInitialInput(workflowId, plan, input);

        return orchestrator.executeWithTrace(plan, input, plan.typeMetadata())
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    private static void validateInitialInput(String workflowId, WorkflowExecutionPlan plan, Object input) {
        Set<Class<?>> rootInputTypes = new LinkedHashSet<>();
        for (TaskNode root : plan.roots()) {
            TypeMetadata metadata = plan.typeMetadata().get(root.id());
            Class<?> inputType = metadata != null ? metadata.inputType() : root.descriptor().task().inputType();
            rootInputTypes.add(inputType);
        }

        boolean requiresInput = rootInputTypes.stream().anyMatch(type -> type != Void.class);
        if (!requiresInput) {
            // Backward-compatible behavior: ignore extra initial input when roots are Void.
            return;
        }

        if (input == null) {
            throw new IllegalArgumentException(
                    "Workflow '" + workflowId + "' requires initial input for root task(s): " + rootInputTypes
            );
        }

        for (Class<?> rootType : rootInputTypes) {
            if (rootType == Void.class) {
                continue;
            }
            if (!rootType.isInstance(input)) {
                throw new IllegalArgumentException(
                        "Workflow '" + workflowId + "' expects initial input compatible with "
                                + rootType.getName() + " but received " + input.getClass().getName()
                );
            }
        }
    }
}
