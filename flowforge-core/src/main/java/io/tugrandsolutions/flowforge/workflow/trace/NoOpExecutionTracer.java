package io.tugrandsolutions.flowforge.workflow.trace;

import java.util.Collections;

/**
 * No-op implementation of {@link ExecutionTracer}.
 */
public final class NoOpExecutionTracer implements ExecutionTracer {

    @Override
    public void onWorkflowStart(String workflowId, String executionId) {}

    @Override
    public void onWorkflowSuccess() {}

    @Override
    public void onWorkflowError(Throwable error) {}

    @Override
    public void onWorkflowCanceled() {}

    @Override
    public void onTaskStart(String taskId) {}

    @Override
    public void onTaskSuccess(String taskId, Object output) {}

    @Override
    public void onTaskSkipped(String taskId) {}

    @Override
    public void onTaskError(String taskId, Throwable error) {}

    @Override
    public ExecutionTrace build() {
        long now = System.currentTimeMillis();
        return new ExecutionTrace(Collections.emptyList(), now, now);
    }
}
