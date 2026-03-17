package io.tugrandsolutions.flowforge.workflow.orchestrator;

/**
 * Control strategies for workflow-level backpressure.
 */
public enum BackpressureStrategy {
    /**
     * Rejects the task immediately if the buffer is full.
     */
    FAIL_FAST,

    /**
     * Pauses the emission of new tasks until the buffer has space.
     */
    BLOCK,

    /**
     * Drops the least recently added task to make room.
     */
    DROP_OLDEST,

    /**
     * Drops the task being added if the buffer is full.
     */
    DROP_LATEST
}
