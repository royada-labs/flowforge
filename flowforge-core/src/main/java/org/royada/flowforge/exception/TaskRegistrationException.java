package org.royada.flowforge.exception;

/**
 * Thrown when task registration fails (duplicate, missing, invalid).
 */
public class TaskRegistrationException extends FlowForgeException {
    /**
     * Creates a new exception with the given message.
     * 
     * @param message the error message
     */
    public TaskRegistrationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     * 
     * @param message the error message
     * @param cause the cause of the failure
     */
    public TaskRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
