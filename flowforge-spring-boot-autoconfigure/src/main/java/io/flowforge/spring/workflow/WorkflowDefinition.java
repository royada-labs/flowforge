package io.flowforge.spring.workflow;

import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Class-based workflow definition contract.
 */
public interface WorkflowDefinition {

    WorkflowExecutionPlan define(FlowDsl dsl);
}
