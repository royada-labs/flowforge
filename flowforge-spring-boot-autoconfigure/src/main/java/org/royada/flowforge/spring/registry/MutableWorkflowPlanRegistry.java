package org.royada.flowforge.spring.registry;

import org.royada.flowforge.registry.WorkflowPlanRegistry;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Mutable extension of {@link WorkflowPlanRegistry} used during bootstrapping.
 */
public interface MutableWorkflowPlanRegistry extends WorkflowPlanRegistry {

    /**
     * Registers a workflow execution plan for the provided id.
     *
     * @param workflowId workflow identifier
     * @param plan execution plan
     */
    void register(String workflowId, WorkflowExecutionPlan plan);
}