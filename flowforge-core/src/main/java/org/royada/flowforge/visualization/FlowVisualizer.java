package org.royada.flowforge.visualization;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.validation.FlowValidationResult;
import org.royada.flowforge.validation.TypeMetadata;
import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for generating a {@link FlowVisualization} from a
 * {@link WorkflowExecutionPlan} and {@link FlowValidationResult}.
 */
public final class FlowVisualizer {

    private FlowVisualizer() {
        // utility
    }

    /**
     * Visualizes a workflow plan, with structural details and associated validation errors.
     *
     * @param plan       the workflow execution plan; must not be null
     * @param validation the validation result; must not be null
     * @return the visualization component
     */
    public static FlowVisualization visualize(
            WorkflowExecutionPlan plan,
            FlowValidationResult validation
    ) {
        return visualize(plan, validation, Collections.emptyMap());
    }

    /**
     * Visualizes a workflow plan with full structural and type information.
     *
     * @param plan       the workflow execution plan; must not be null
     * @param validation the validation result; must not be null
     * @param typeInfo   the collected type metadata; must not be null
     * @return the visualization component
     */
    public static FlowVisualization visualize(
            WorkflowExecutionPlan plan,
            FlowValidationResult validation,
            Map<TaskId, TypeMetadata> typeInfo
    ) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(validation, "validation");
        Objects.requireNonNull(typeInfo, "typeInfo");

        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // Sort nodes by taskId for deterministic output
        List<TaskNode> sortedNodes = plan.nodes().stream()
                .sorted(Comparator.comparing(n -> n.id().getValue()))
                .toList();

        for (TaskNode node : sortedNodes) {
            String id = node.id().getValue();
            TypeMetadata types = typeInfo.get(node.id());

            nodes.add(new VisualNode(
                    id,
                    types != null ? types.inputType().getSimpleName() : "Unknown",
                    types != null ? types.outputType().getSimpleName() : "Unknown",
                    node.isRoot(),
                    node.isLeaf()
            ));

            // Populate edges
            for (TaskNode dependent : node.dependents()) {
                edges.add(new VisualEdge(id, dependent.id().getValue()));
            }
        }

        // Deterministic edge order
        edges.sort(Comparator.comparing(VisualEdge::from).thenComparing(VisualEdge::to));

        // Errors
        List<VisualError> errors = validation.errors().stream()
                .map(e -> new VisualError(
                        e.code(),
                        e.message(),
                        e.taskId(),
                        e.severity().name()
                ))
                .collect(Collectors.toList());

        return new FlowVisualization(nodes, edges, errors);
    }
}
