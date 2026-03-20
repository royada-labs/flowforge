package org.royada.flowforge.registry;

import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Unified internal representation of a workflow definition.
 */
public interface WorkflowDescriptor {

    /**
     * Returns the unique ID of the workflow.
     * 
     * @return the workflow ID
     */
    String id();

    /**
     * Returns the execution plan for the workflow.
     * 
     * @return the plan
     */
    WorkflowExecutionPlan plan();

    /**
     * Returns the source class that defined the workflow.
     * 
     * @return the source class
     */
    Class<?> source();

    /**
     * Returns metadata associated with the workflow.
     * 
     * @return the metadata
     */
    default String metadata() {
        return "";
    }

    /**
     * Returns the version of the workflow.
     * 
     * @return the version
     */
    default String version() {
        return "";
    }
}
