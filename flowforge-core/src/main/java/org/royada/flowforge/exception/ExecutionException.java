package org.royada.flowforge.exception;

/**
 * Thrown when an error occurs during the execution of a task or workflow.
 */
public class ExecutionException extends FlowForgeException {
    /**
     * Creates a new execution exception with the given message.
     * 
     * @param message exception message
     */
    public ExecutionException(String message) {
        super(message);
    }

    /**
     * Creates a new execution exception with the given message and cause.
     * 
     * @param message exception message
     * @param cause root cause
     */
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
