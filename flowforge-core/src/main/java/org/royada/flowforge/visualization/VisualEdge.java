package org.royada.flowforge.visualization;

import java.util.Objects;

/**
 * Represents a connection (dependency) between two nodes in the workflow.
 *
 * <p>In a standard DAG, this implies {@code from -> to} where {@code from}
 * must complete before {@code to} can execute.
 *
 * @param from the source taskId
 * @param to   the destination taskId
 */
public record VisualEdge(String from, String to) {
    /**
     * Creates a new visual edge and validates its fields.
     * 
     * @param from the source taskId
     * @param to the destination taskId
     */
    public VisualEdge {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }
}
