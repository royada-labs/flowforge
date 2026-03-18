package io.flowforge.spring.registry;

import io.flowforge.registry.WorkflowPlanRegistry;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

public interface MutableWorkflowPlanRegistry extends WorkflowPlanRegistry {

    void register(String workflowId, WorkflowExecutionPlan plan);
}