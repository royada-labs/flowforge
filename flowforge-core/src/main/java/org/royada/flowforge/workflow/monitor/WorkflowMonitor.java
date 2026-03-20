package org.royada.flowforge.workflow.monitor;

import java.time.Duration;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
import org.royada.flowforge.workflow.report.ExecutionReport;

public interface WorkflowMonitor {

    default void onWorkflowStart(WorkflowInstance instance) {
    }

    default void onWorkflowComplete(WorkflowInstance instance) {
    }

    /**
     * Called when workflow completes with execution report.
     * Default implementation delegates to onWorkflowComplete(instance) for backward
     * compatibility.
     */
    default void onWorkflowComplete(WorkflowInstance instance, ExecutionReport report) {
        onWorkflowComplete(instance);
    }

    default void onTaskStart(WorkflowInstance instance, TaskId taskId) {
    }

    default void onTaskSuccess(WorkflowInstance instance, TaskId taskId) {
    }

    /**
     * Called when a task completes successfully with duration information.
     * Default implementation delegates to onTaskSuccess(instance, taskId) for
     * backward compatibility.
     */
    default void onTaskSuccess(WorkflowInstance instance, TaskId taskId, Duration duration) {
        onTaskSuccess(instance, taskId);
    }

    default void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {
    }

    /**
     * Called when a task is skipped with duration information (time until skip
     * decision).
     * Default implementation delegates to onTaskSkipped(instance, taskId) for
     * backward compatibility.
     */
    default void onTaskSkipped(WorkflowInstance instance, TaskId taskId, Duration duration) {
        onTaskSkipped(instance, taskId);
    }

    default void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error) {
    }

    /**
     * Called when a task fails with duration information.
     * Default implementation delegates to onTaskFailure(instance, taskId, error)
     * for backward compatibility.
     */
    default void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error, Duration duration) {
        onTaskFailure(instance, taskId, error);
    }
}