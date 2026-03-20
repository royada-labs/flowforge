package org.royada.flowforge.exception;

/**
 * Base exception for all FlowForge related errors.
 */
public class FlowForgeException extends RuntimeException {
    /**
     * @param message exception message
     */
    public FlowForgeException(String message) {
        super(message);
    }

    /**
     * @param message exception message
     * @param cause root cause
     */
    public FlowForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
