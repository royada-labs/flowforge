package org.royada.flowforge.registry;

import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Unified internal representation of a workflow definition.
 */
public interface WorkflowDescriptor {

    String id();

    WorkflowExecutionPlan plan();

    Class<?> source();

    default String metadata() {
        return "";
    }

    default String version() {
        return "";
    }
}
