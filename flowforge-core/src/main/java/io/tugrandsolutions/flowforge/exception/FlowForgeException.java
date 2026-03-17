package io.tugrandsolutions.flowforge.exception;

/**
 * Base exception for all FlowForge related errors.
 */
public class FlowForgeException extends RuntimeException {
    public FlowForgeException(String message) {
        super(message);
    }

    public FlowForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
