package org.royada.flowforge.spring.workflow;

import org.royada.flowforge.spring.dsl.FlowDsl;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Class-based workflow definition contract.
 */
public interface WorkflowDefinition {

    /**
     * Defines and returns the workflow execution plan.
     *
     * @param dsl flow DSL entry point
     * @return workflow execution plan
     */
    WorkflowExecutionPlan define(FlowDsl dsl);
}
