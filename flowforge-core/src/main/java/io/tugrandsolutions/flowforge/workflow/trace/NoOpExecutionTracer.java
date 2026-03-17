package io.tugrandsolutions.flowforge.workflow.trace;

import java.util.Collections;

/**
 * Empty implementation of {@link ExecutionTracer} that does nothing.
 */
public final class NoOpExecutionTracer implements ExecutionTracer {
    @Override public void onTaskStart(String taskId) {}
    @Override public void onTaskSuccess(String taskId, Object output) {}
    @Override public void onTaskSkipped(String taskId) {}
    @Override public void onTaskError(String taskId, Throwable error) {}

    @Override
    public ExecutionTrace build() {
        return new ExecutionTrace(Collections.emptyList(), 0, 0);
    }
}
