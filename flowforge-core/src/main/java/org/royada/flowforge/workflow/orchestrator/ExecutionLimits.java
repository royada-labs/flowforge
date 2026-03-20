package org.royada.flowforge.workflow.orchestrator;

import java.util.Objects;

/**
 * Global or per-orchestrator execution limits.
 *
 * @param maxInFlightTasks maximum number of tasks executing concurrently
 * @param maxQueueSize maximum number of tasks allowed in queue
 * @param backpressureStrategy strategy applied when queue is full
 */
public record ExecutionLimits(
    /** Maximum number of tasks in-flight across a single workflow. */
    int maxInFlightTasks,

    /** Maximum size of the internal task pending queue. */
    int maxQueueSize,

    /** Backpressure strategy when the queue is full. */
    BackpressureStrategy backpressureStrategy
) {
    /**
     * @return default execution limits
     */
    public static ExecutionLimits defaultLimits() {
        return new ExecutionLimits(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            1000,
            BackpressureStrategy.BLOCK
        );
    }

    /**
     * Validates record components.
     */
    public ExecutionLimits {
        if (maxInFlightTasks <= 0) throw new IllegalArgumentException("maxInFlightTasks must be > 0");
        if (maxQueueSize <= 0) throw new IllegalArgumentException("maxQueueSize must be > 0");
        Objects.requireNonNull(backpressureStrategy, "backpressureStrategy");
    }
}
