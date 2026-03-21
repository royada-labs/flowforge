package org.royada.flowforge.exception;

/**
 * Base exception for all FlowForge related errors.
 */
public class FlowForgeException extends RuntimeException {
    /**
     * Creates a new exception with the given message.
     * 
     * @param message exception message
     */
    public FlowForgeException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     * 
     * @param message exception message
     * @param cause root cause
     */
    public FlowForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
