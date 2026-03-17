package io.flowforge.exception;

/**
 * Thrown when an error occurs during the execution of a task or workflow.
 */
public class ExecutionException extends FlowForgeException {
    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
