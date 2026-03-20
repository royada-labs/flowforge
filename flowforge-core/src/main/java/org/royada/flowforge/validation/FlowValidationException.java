package org.royada.flowforge.validation;

import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Objects;
import java.util.Optional;

/**
 * Thrown when a workflow definition fails validation.
 *
 * <p>The exception message is formatted in compiler style, listing all
 * errors and warnings for easy diagnosis:
 *
 * <pre>{@code
 * FlowForge workflow validation found 1 error(s), 0 warning(s):
 *   1. [TYPE_MISMATCH] Task 'enrichUser': expected UserProfile but got Order (from 'fetchOrder')
 * }</pre>
 */
public class FlowValidationException extends RuntimeException {

    private final FlowValidationResult result;
    private final WorkflowExecutionPlan plan;

    /**
     * Creates a validation exception from the given result.
     *
     * @param result the validation result containing errors; must not be null
     */
    public FlowValidationException(FlowValidationResult result) {
        this(result, null);
    }

    /**
     * Creates a validation exception from the given result and the plan that failed validation.
     *
     * @param result the validation result containing errors; must not be null
     * @param plan   the plan that failed validation; may be null if the plan couldn't be built
     */
    public FlowValidationException(FlowValidationResult result, WorkflowExecutionPlan plan) {
        super(Objects.requireNonNull(result, "result").formatted());
        this.result = result;
        this.plan = plan;
    }

    /**
     * Returns the validation result that caused this exception.
     *
     * @return the validation result
     */
    public FlowValidationResult result() {
        return result;
    }

    /**
     * Returns the (invalid) plan that caused this validation failure, if available.
     *
     * @return the plan optional
     */
    public Optional<WorkflowExecutionPlan> plan() {
        return Optional.ofNullable(plan);
    }
}
