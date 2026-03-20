package org.royada.flowforge.registry;

import java.util.Collection;
import java.util.Optional;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

public interface WorkflowPlanRegistry {

    boolean contains(String workflowId);

    Optional<WorkflowExecutionPlan> find(String workflowId);

    Collection<WorkflowExecutionPlan> snapshot();
}