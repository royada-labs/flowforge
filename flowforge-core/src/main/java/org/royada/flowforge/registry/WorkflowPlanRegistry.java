package org.royada.flowforge.registry;

import java.util.Collection;
import java.util.Optional;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Interface for a registry that provides access to {@link WorkflowExecutionPlan} instances.
 */
public interface WorkflowPlanRegistry {

    /**
     * Returns whether the registry contains a plan for the given workflow ID.
     * 
     * @param workflowId the workflow ID
     * @return {@code true} if found, {@code false} otherwise
     */
    boolean contains(String workflowId);

    /**
     * Finds the execution plan for the given workflow ID.
     * 
     * @param workflowId the workflow ID
     * @return an optional containing the plan, or empty if not found
     */
    Optional<WorkflowExecutionPlan> find(String workflowId);

    /**
     * Returns a snapshot of all execution plans currently in the registry.
     * 
     * @return a collection of all plans
     */
    Collection<WorkflowExecutionPlan> snapshot();
}