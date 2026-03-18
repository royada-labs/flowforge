package io.flowforge.workflow.trace;

import io.flowforge.task.TaskId;

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
    public void onTaskStart(TaskId taskId, java.util.Collection<TaskId> dependencyIds) {}

    @Override
    public void onTaskSuccess(TaskId taskId, Object output) {}

    @Override
    public void onTaskSkipped(TaskId taskId) {}

    @Override
    public void onTaskError(TaskId taskId, Throwable error) {}

    @Override
    public ExecutionTrace build() {
        long now = System.currentTimeMillis();
        return new ExecutionTrace(Collections.emptyList(), now, now);
    }
}
