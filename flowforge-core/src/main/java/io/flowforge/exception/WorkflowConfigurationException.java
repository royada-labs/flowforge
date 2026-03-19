package io.flowforge.exception;

/**
 * Thrown when a workflow or task configuration is invalid.
 */
public class WorkflowConfigurationException extends FlowForgeException {
    public WorkflowConfigurationException(String message) {
        super(message);
    }

    public WorkflowConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
