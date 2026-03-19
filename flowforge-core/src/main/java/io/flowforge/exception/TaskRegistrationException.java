package io.flowforge.exception;

/**
 * Thrown when task registration fails (duplicate, missing, invalid).
 */
public class TaskRegistrationException extends FlowForgeException {
    public TaskRegistrationException(String message) {
        super(message);
    }

    public TaskRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
