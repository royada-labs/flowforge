package io.flowforge.visualization;

import java.util.Objects;

/**
 * Represents a node in the visual representation of a workflow.
 *
 * @param taskId     the unique identifier of the task
 * @param inputType  the declared input type (e.g., "String", "Void")
 * @param outputType the declared output type (e.g., "Integer", "UserProfile")
 * @param isRoot     whether this task is a starting point of the workflow
 * @param isTerminal whether this task has no downstream dependencies
 */
public record VisualNode(
        String taskId,
        String inputType,
        String outputType,
        boolean isRoot,
        boolean isTerminal
) {
    public VisualNode {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(inputType, "inputType");
        Objects.requireNonNull(outputType, "outputType");
    }
}
