package io.flowforge.validation;

import io.flowforge.task.TaskId;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.List;
import java.util.Map;

/**
 * A single validation rule that can be applied to a workflow execution plan.
 *
 * <p>Implement this interface to extend the validator with custom rules:
 * <pre>{@code
 * FlowValidationRule noSelfLoops = (plan, types) -> { ... };
 * validator.addRule(noSelfLoops);
 * }</pre>
 */
@FunctionalInterface
public interface FlowValidationRule {

    /**
     * Validates the given plan and returns any errors found.
     *
     * @param plan     the materialized workflow plan; never null
     * @param typeInfo type metadata collected during typed DSL usage;
     *                 may be empty for legacy string-based workflows
     * @return a list of validation errors (empty if the rule passes)
     */
    List<FlowValidationError> validate(WorkflowExecutionPlan plan, Map<TaskId, TypeMetadata> typeInfo);
}
