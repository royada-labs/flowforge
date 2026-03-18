package io.flowforge.visualization;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable representation of a workflow's DAG for architectural inspection
 * and documentation.
 *
 * <p>Provides exporters to JSON and Mermaid formats.
 */
public final class FlowVisualization {

    private final List<VisualNode> nodes;
    private final List<VisualEdge> edges;
    private final List<VisualError> errors;

    /**
     * Creates a new visualization from the given lists.
     *
     * @param nodes  the list of visual nodes; must not be null
     * @param edges  the list of visual connections; must not be null
     * @param errors the list of associated validation results; must not be null
     */
    public FlowVisualization(
            List<VisualNode> nodes,
            List<VisualEdge> edges,
            List<VisualError> errors
    ) {
        this.nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        this.edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
    }

    public List<VisualNode> nodes() { return nodes; }
    public List<VisualEdge> edges() { return edges; }
    public List<VisualError> errors() { return errors; }

    /**
     * Exports the graph to a JSON string.
     *
     * @return the JSON representation
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Nodes
        sb.append("  \"nodes\": [\n");
        for (int i = 0; i < nodes.size(); i++) {
            VisualNode node = nodes.get(i);
            sb.append("    {\n");
            sb.append("      \"taskId\": \"").append(node.taskId()).append("\",\n");
            sb.append("      \"inputType\": \"").append(node.inputType()).append("\",\n");
            sb.append("      \"outputType\": \"").append(node.outputType()).append("\",\n");
            sb.append("      \"root\": ").append(node.isRoot()).append(",\n");
            sb.append("      \"terminal\": ").append(node.isTerminal()).append("\n");
            sb.append("    }").append(i < nodes.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // Edges
        sb.append("  \"edges\": [\n");
        for (int i = 0; i < edges.size(); i++) {
            VisualEdge edge = edges.get(i);
            sb.append("    {\n");
            sb.append("      \"from\": \"").append(edge.from()).append("\",\n");
            sb.append("      \"to\": \"").append(edge.to()).append("\"\n");
            sb.append("    }").append(i < edges.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // Errors
        sb.append("  \"errors\": [\n");
        for (int i = 0; i < errors.size(); i++) {
            VisualError error = errors.get(i);
            sb.append("    {\n");
            sb.append("      \"code\": \"").append(error.code()).append("\",\n");
            sb.append("      \"taskId\": \"").append(error.taskId()).append("\",\n");
            sb.append("      \"message\": \"").append(error.message().replace("\"", "\\\"")).append("\",\n");
            sb.append("      \"severity\": \"").append(error.severity()).append("\"\n");
            sb.append("    }").append(i < errors.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Exports the graph to a Mermaid string.
     *
     * @return the Mermaid markup
     */
    public String toMermaid() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");

        // Node definitions (with types)
        for (VisualNode node : nodes) {
            sb.append("    ").append(node.taskId())
                    .append("[").append(node.taskId())
                    .append("\\n(").append(node.inputType())
                    .append(" → ").append(node.outputType())
                    .append(")]\n");
        }

        sb.append("\n");

        // Edge definitions
        for (VisualEdge edge : edges) {
            sb.append("    ").append(edge.from())
                    .append(" --> ")
                    .append(edge.to()).append("\n");
        }

        sb.append("\n");

        // Styling based on errors
        Set<String> nodesWithErrors = errors.stream()
                .filter(e -> "ERROR".equals(e.severity()))
                .map(VisualError::taskId)
                .collect(Collectors.toSet());

        Set<String> nodesWithWarnings = errors.stream()
                .filter(e -> "WARNING".equals(e.severity()))
                .map(VisualError::taskId)
                .collect(Collectors.toSet());

        for (String id : nodesWithErrors) {
            sb.append("    class ").append(id).append(" ff-error\n");
        }

        for (String id : nodesWithWarnings) {
            // Error style takes precedence over warning style
            if (!nodesWithErrors.contains(id)) {
                sb.append("    class ").append(id).append(" ff-warning\n");
            }
        }

        sb.append("\n");
        sb.append("    classDef ff-error fill:#ffcccc,stroke:#ff0000,stroke-width:2px;\n");
        sb.append("    classDef ff-warning fill:#fff3cd,stroke:#ffcc00,stroke-width:1px;\n");

        return sb.toString();
    }
}
