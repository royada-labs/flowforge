package io.tugrandsolutions.flowforge.validation;

import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.*;

/**
 * Default implementation of {@link FlowValidator} with built-in rules for
 * structural and type-level validation.
 *
 * <p>Built-in rules:
 * <ol>
 *   <li><b>UNREACHABLE_NODE</b> — detects nodes not reachable from any root</li>
 *   <li><b>TYPE_MISMATCH</b> — validates type compatibility between connected tasks</li>
 *   <li><b>MISSING_INPUT</b> — root tasks that declare a non-Void input type</li>
 *   <li><b>UNUSED_OUTPUT</b> — leaf tasks with non-Void output (warning only)</li>
 * </ol>
 *
 * <p>Custom rules can be added via {@link #addRule(FlowValidationRule)}:
 * <pre>{@code
 * DefaultFlowValidator validator = new DefaultFlowValidator();
 * validator.addRule((plan, types) -> { ... });
 * }</pre>
 */
public final class DefaultFlowValidator implements FlowValidator {

    private final List<FlowValidationRule> rules;

    /**
     * Creates a validator with all built-in rules.
     */
    public DefaultFlowValidator() {
        this.rules = new ArrayList<>();
        this.rules.add(new UnreachableNodeRule());
        this.rules.add(new TypeCompatibilityRule());
        this.rules.add(new MissingInputRule());
        this.rules.add(new UnusedOutputRule());
    }

    /**
     * Adds a custom validation rule.
     *
     * @param rule the rule to add; must not be null
     * @return this validator (for chaining)
     */
    public DefaultFlowValidator addRule(FlowValidationRule rule) {
        Objects.requireNonNull(rule, "rule");
        rules.add(rule);
        return this;
    }

    @Override
    public FlowValidationResult validate(WorkflowExecutionPlan plan, Map<String, TypeMetadata> typeInfo) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(typeInfo, "typeInfo");

        List<FlowValidationError> allErrors = new ArrayList<>();

        for (FlowValidationRule rule : rules) {
            allErrors.addAll(rule.validate(plan, typeInfo));
        }

        return FlowValidationResult.of(allErrors);
    }

    // =========================================================================
    // Built-in rules
    // =========================================================================

    /**
     * Detects nodes that are not reachable from any root via forward traversal.
     *
     * <p>A node is reachable if there exists a path from any root to it,
     * following the dependency → dependent direction.
     */
    static final class UnreachableNodeRule implements FlowValidationRule {
        @Override
        public List<FlowValidationError> validate(WorkflowExecutionPlan plan, Map<String, TypeMetadata> typeInfo) {
            Set<TaskId> reachable = new HashSet<>();
            Deque<TaskNode> queue = new ArrayDeque<>(plan.roots());

            while (!queue.isEmpty()) {
                TaskNode current = queue.poll();
                if (reachable.add(current.id())) {
                    for (TaskNode dependent : current.dependents()) {
                        if (!reachable.contains(dependent.id())) {
                            queue.add(dependent);
                        }
                    }
                }
            }

            List<FlowValidationError> errors = new ArrayList<>();
            for (TaskNode node : plan.nodes()) {
                if (!reachable.contains(node.id())) {
                    errors.add(FlowValidationError.error(
                            FlowValidationError.UNREACHABLE_NODE,
                            node.id().getValue(),
                            "Node is not reachable from any root in the DAG"
                    ));
                }
            }
            return errors;
        }
    }

    /**
     * Validates type compatibility between connected tasks.
     *
     * <p>For each edge (upstream → downstream) where both tasks have type
     * metadata, verifies that the upstream's output type is assignable to
     * the downstream's input type.
     */
    static final class TypeCompatibilityRule implements FlowValidationRule {
        @Override
        public List<FlowValidationError> validate(WorkflowExecutionPlan plan, Map<String, TypeMetadata> typeInfo) {
            if (typeInfo.isEmpty()) {
                return List.of();
            }

            List<FlowValidationError> errors = new ArrayList<>();

            for (TaskNode node : plan.nodes()) {
                String nodeId = node.id().getValue();
                TypeMetadata nodeType = typeInfo.get(nodeId);
                if (nodeType == null) {
                    continue; // no type info for this task — skip
                }

                for (TaskNode dependency : node.dependencies()) {
                    String depId = dependency.id().getValue();
                    TypeMetadata depType = typeInfo.get(depId);
                    if (depType == null) {
                        continue; // upstream has no type info — skip
                    }

                    if (!nodeType.inputType().isAssignableFrom(depType.outputType())) {
                        errors.add(FlowValidationError.error(
                                FlowValidationError.TYPE_MISMATCH,
                                nodeId,
                                "expected input type " + nodeType.inputType().getSimpleName()
                                        + " but got " + depType.outputType().getSimpleName()
                                        + " (from '" + depId + "')"
                        ));
                    }
                }
            }
            return errors;
        }
    }

    /**
     * Detects root tasks (no dependencies) that declare a non-Void input type,
     * meaning they expect input but will receive none.
     */
    static final class MissingInputRule implements FlowValidationRule {
        @Override
        public List<FlowValidationError> validate(WorkflowExecutionPlan plan, Map<String, TypeMetadata> typeInfo) {
            if (typeInfo.isEmpty()) {
                return List.of();
            }

            List<FlowValidationError> errors = new ArrayList<>();

            for (TaskNode root : plan.roots()) {
                String rootId = root.id().getValue();
                TypeMetadata rootType = typeInfo.get(rootId);
                if (rootType == null) {
                    continue;
                }

                if (rootType.inputType() != Void.class) {
                    errors.add(FlowValidationError.error(
                            FlowValidationError.MISSING_INPUT,
                            rootId,
                            "Root task expects input type " + rootType.inputType().getSimpleName()
                                    + " but has no upstream dependency"
                    ));
                }
            }
            return errors;
        }
    }

    /**
     * Reports leaf tasks (no dependents) whose output type is non-Void but
     * is never consumed. This is a WARNING — it does not block execution.
     */
    static final class UnusedOutputRule implements FlowValidationRule {
        @Override
        public List<FlowValidationError> validate(WorkflowExecutionPlan plan, Map<String, TypeMetadata> typeInfo) {
            if (typeInfo.isEmpty()) {
                return List.of();
            }

            List<FlowValidationError> errors = new ArrayList<>();

            for (TaskNode node : plan.nodes()) {
                if (!node.isLeaf()) {
                    continue;
                }

                String nodeId = node.id().getValue();
                TypeMetadata nodeType = typeInfo.get(nodeId);
                if (nodeType == null) {
                    continue;
                }

                if (nodeType.outputType() != Void.class) {
                    errors.add(FlowValidationError.warning(
                            FlowValidationError.UNUSED_OUTPUT,
                            nodeId,
                            "Leaf task produces " + nodeType.outputType().getSimpleName()
                                    + " but no downstream task consumes it"
                    ));
                }
            }
            return errors;
        }
    }
}
