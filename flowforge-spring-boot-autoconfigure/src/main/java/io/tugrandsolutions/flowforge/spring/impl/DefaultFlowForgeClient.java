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

                return orchestrator.execute(workflowId, plan, input)
                                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
        }

        @Override
        public Mono<ReactiveExecutionContext> execute(String workflowId, Object input, Duration timeout) {
                WorkflowExecutionPlan plan = planRegistry.find(workflowId)
                                .orElseThrow(() -> new UnknownWorkflowException(workflowId));

                // We need to support timeout on the orchestrator as well if we want full
                // parity,
                // otherwise we can just use the Mono.timeout() operator manually here,
                // but orchestrator.execute(plan, input, timeout) is what was used.
                // Let's check if orchestrator has an overload for (id, plan, input, timeout).

                // Assuming we didn't add the overload in ReactiveWorkflowOrchestrator yet for
                // (id, plan, input, timeout).
                // Let's check ReactiveWorkflowOrchestrator again or just use operator chain
                // since the orchestrator overload just does .timeout() anyway.
                // But to keep consistency with the previous pattern:
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
}
