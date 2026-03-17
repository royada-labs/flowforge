package io.flowforge.visualization;

import java.util.Objects;

/**
 * Represents a validation problem associated with a specific task in the visual
 * representation of the workflow.
 *
 * @param code     the validation code (e.g., "TYPE_MISMATCH")
 * @param message  human-readable description
 * @param taskId   the id of the affected task
 * @param severity severity level ("ERROR" or "WARNING")
 */
public record VisualError(
        String code,
        String message,
        String taskId,
        String severity
) {
    public VisualError {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(severity, "severity");
    }
}
