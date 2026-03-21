package org.royada.flowforge.workflow.monitor;

import java.time.Duration;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
import org.royada.flowforge.workflow.report.ExecutionReport;

/**
 * Hook interface for observing workflow and task lifecycle events.
 */
public interface WorkflowMonitor {

    /**
     * Called when workflow execution starts.
     *
     * @param instance workflow instance
     */
    default void onWorkflowStart(WorkflowInstance instance) {
    }

    /**
     * Called when workflow execution completes.
     *
     * @param instance workflow instance
     */
    default void onWorkflowComplete(WorkflowInstance instance) {
    }

    /**
     * Called when workflow completes with execution report.
     * Default implementation delegates to onWorkflowComplete(instance) for backward
     * compatibility.
     *
     * @param instance workflow instance
     * @param report execution report
     */
    default void onWorkflowComplete(WorkflowInstance instance, ExecutionReport report) {
        onWorkflowComplete(instance);
    }

    /**
     * Called when a task starts.
     *
     * @param instance workflow instance
     * @param taskId task id
     */
    default void onTaskStart(WorkflowInstance instance, TaskId taskId) {
    }

    /**
     * Called when a task succeeds.
     *
     * @param instance workflow instance
     * @param taskId task id
     */
    default void onTaskSuccess(WorkflowInstance instance, TaskId taskId) {
    }

    /**
     * Called when a task completes successfully with duration information.
     * Default implementation delegates to onTaskSuccess(instance, taskId) for
     * backward compatibility.
     *
     * @param instance workflow instance
     * @param taskId task id
     * @param duration task duration
     */
    default void onTaskSuccess(WorkflowInstance instance, TaskId taskId, Duration duration) {
        onTaskSuccess(instance, taskId);
    }

    /**
     * Called when a task is skipped.
     *
     * @param instance workflow instance
     * @param taskId task id
     */
    default void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {
    }

    /**
     * Called when a task is skipped with duration information (time until skip
     * decision).
     * Default implementation delegates to onTaskSkipped(instance, taskId) for
     * backward compatibility.
     *
     * @param instance workflow instance
     * @param taskId task id
     * @param duration task duration
     */
    default void onTaskSkipped(WorkflowInstance instance, TaskId taskId, Duration duration) {
        onTaskSkipped(instance, taskId);
    }

    /**
     * Called when a task fails.
     *
     * @param instance workflow instance
     * @param taskId task id
     * @param error task error
     */
    default void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error) {
    }

    /**
     * Called when a task fails with duration information.
     * Default implementation delegates to onTaskFailure(instance, taskId, error)
     * for backward compatibility.
     *
     * @param instance workflow instance
     * @param taskId task id
     * @param error task error
     * @param duration task duration
     */
    default void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error, Duration duration) {
        onTaskFailure(instance, taskId, error);
    }
}