package io.flowforge.validation;

import io.flowforge.task.TaskId;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Map;

/**
 * Validates a {@link WorkflowExecutionPlan} before execution, detecting
 * structural and type-level errors.
 *
 * @see DefaultFlowValidator
 */
public interface FlowValidator {

    /**
     * Validates the plan using structural rules only (no type metadata).
     *
     * @param plan the workflow execution plan; must not be null
     * @return the validation result
     */
    default FlowValidationResult validate(WorkflowExecutionPlan plan) {
        return validate(plan, Map.of());
    }

    /**
     * Validates the plan using both structural and type-level rules.
     *
     * @param plan     the workflow execution plan; must not be null
     * @param typeInfo type metadata keyed by task id; may be empty
     * @return the validation result
     */
    FlowValidationResult validate(WorkflowExecutionPlan plan, Map<TaskId, TypeMetadata> typeInfo);
}
