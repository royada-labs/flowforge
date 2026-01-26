package io.tugrandsolutions.flowforge.spring.registry;

import io.tugrandsolutions.flowforge.registry.WorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

public interface MutableWorkflowPlanRegistry extends WorkflowPlanRegistry {

    void register(String workflowId, WorkflowExecutionPlan plan);
}