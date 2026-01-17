package io.tugrandsolutions.flowforge.spring.impl;

import java.time.Duration;
import java.util.Objects;

import io.tugrandsolutions.flowforge.spring.api.FlowForgeClient;
import io.tugrandsolutions.flowforge.spring.exception.UnknownWorkflowException;
import io.tugrandsolutions.flowforge.spring.exception.WorkflowExecutionException;
import io.tugrandsolutions.flowforge.spring.registry.WorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
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

                return orchestrator.execute(plan, input)
                                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
        }

        @Override
        public Mono<ReactiveExecutionContext> execute(String workflowId, Object input, Duration timeout) {
                WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                                .orElseThrow(() -> new UnknownWorkflowException(workflowId));

                return orchestrator.execute(plan, input, timeout)
                                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
        }
}
