package org.royada.flowforge.workflow.trace;

import org.royada.flowforge.task.TaskId;

import java.util.List;
import java.util.Objects;

/**
 * Composites multiple tracers into a single one.
 * Useful for having both internal metrics/debug logs and external OpenTelemetry spans.
 */
public final class CompositeExecutionTracer implements ExecutionTracer {

    private final List<ExecutionTracer> tracers;

    /**
     * @param tracers delegate tracers
     */
    public CompositeExecutionTracer(List<ExecutionTracer> tracers) {
        this.tracers = List.copyOf(Objects.requireNonNull(tracers, "tracers"));
    }

    @Override
    public void onWorkflowStart(String workflowId, String executionId) {
        for (ExecutionTracer tracer : tracers) {
            tracer.onWorkflowStart(workflowId, executionId);
        }
    }

    @Override
    public void onWorkflowSuccess() {
        for (ExecutionTracer tracer : tracers) {
            tracer.onWorkflowSuccess();
        }
    }

    @Override
    public void onWorkflowError(Throwable error) {
        for (ExecutionTracer tracer : tracers) {
            tracer.onWorkflowError(error);
        }
    }

    @Override
    public void onWorkflowCanceled() {
        for (ExecutionTracer tracer : tracers) {
            tracer.onWorkflowCanceled();
        }
    }

    @Override
    public void onTaskStart(TaskId taskId, java.util.Collection<TaskId> dependencyIds) {
        for (ExecutionTracer tracer : tracers) {
            tracer.onTaskStart(taskId, dependencyIds);
        }
    }

    @Override
    public void onTaskSuccess(TaskId taskId, Object output) {
        for (ExecutionTracer tracer : tracers) {
            tracer.onTaskSuccess(taskId, output);
        }
    }

    @Override
    public void onTaskSkipped(TaskId taskId) {
        for (ExecutionTracer tracer : tracers) {
            tracer.onTaskSkipped(taskId);
        }
    }

    @Override
    public void onTaskError(TaskId taskId, Throwable error) {
        for (ExecutionTracer tracer : tracers) {
            tracer.onTaskError(taskId, error);
        }
    }

    @Override
    public ExecutionTrace build() {
        // We favor the first tracer that produces a non-empty trace, 
        // usually the DefaultExecutionTracer.
        for (ExecutionTracer tracer : tracers) {
            ExecutionTrace trace = tracer.build();
            if (!trace.tasks().isEmpty()) {
                return trace;
            }
        }
        // Fallback to empty trace from the first one
        return tracers.isEmpty() ? null : tracers.getFirst().build();
    }
}
