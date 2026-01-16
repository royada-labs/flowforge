package io.tugrandsolutions.flowforge.spring.registry;

import java.util.Collection;
import java.util.Optional;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

public interface WorkflowPlanRegistry {

    boolean contains(String workflowId);

    Optional<WorkflowExecutionPlan> find(String workflowId);

    Collection<WorkflowExecutionPlan> snapshot();
}