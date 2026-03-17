package io.tugrandsolutions.flowforge.impl;

import java.time.Duration;
import java.util.Objects;

import io.tugrandsolutions.flowforge.api.FlowForgeClient;
import io.tugrandsolutions.flowforge.exception.UnknownWorkflowException;
import io.tugrandsolutions.flowforge.exception.WorkflowExecutionException;
import io.tugrandsolutions.flowforge.registry.WorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.tugrandsolutions.flowforge.workflow.trace.ExecutionTrace;
import reactor.core.publisher.Mono;

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

        return orchestrator.execute(workflowId, plan, input)
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<ReactiveExecutionContext> execute(String workflowId, Object input, Duration timeout) {
        WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                .orElseThrow(() -> new UnknownWorkflowException(workflowId));

        return orchestrator.execute(workflowId, plan, input)
                .timeout(timeout)
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<Object> executeResult(String workflowId, Object input) {
        WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                .orElseThrow(() -> new UnknownWorkflowException(workflowId));

        return orchestrator.execute(workflowId, plan, input)
                .map(ctx -> io.tugrandsolutions.flowforge.workflow.result.WorkflowResultSelector
                        .select(plan, ctx))
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<ExecutionTrace> executeWithTrace(String workflowId, Object input) {
        WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                .orElseThrow(() -> new UnknownWorkflowException(workflowId));

        return orchestrator.executeWithTrace(plan, input, plan.typeMetadata())
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }
}
