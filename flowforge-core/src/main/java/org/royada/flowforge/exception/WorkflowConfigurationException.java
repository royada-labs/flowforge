package org.royada.flowforge.exception;

/**
 * Thrown when a workflow or task configuration is invalid.
 */
public class WorkflowConfigurationException extends FlowForgeException {
    /**
     * Creates a new exception with the given message.
     * 
     * @param message the error message
     */
    public WorkflowConfigurationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     * 
     * @param message the error message
     * @param cause the cause of the failure
     */
    public WorkflowConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
