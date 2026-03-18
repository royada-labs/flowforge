package io.flowforge.registry;

import java.util.Collection;
import java.util.Optional;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

public interface WorkflowPlanRegistry {

    boolean contains(String workflowId);

    Optional<WorkflowExecutionPlan> find(String workflowId);

    Collection<WorkflowExecutionPlan> snapshot();
}