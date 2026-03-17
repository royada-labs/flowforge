package io.flowforge.workflow.trace;

import io.flowforge.task.TaskId;

/**
 * Interface for tracing task execution in real-time.
 */
public interface ExecutionTracer {

    /**
     * Called when the entire workflow execution begins.
     *
     * @param workflowId  the id of the workflow; must not be null
     * @param executionId the unique instance id for this run; must not be null
     */
    void onWorkflowStart(String workflowId, String executionId);

    /**
     * Called when the workflow completes successfully.
     */
    void onWorkflowSuccess();

    /**
     * Called when the workflow fails with a fatal error.
     *
     * @param error the fatal exception
     */
    void onWorkflowError(Throwable error);

    /**
     * Called when the workflow execution is canceled.
     */
    void onWorkflowCanceled();

    /**
     * Called when a task starts execution, providing its precursor dependencies.
     *
     * @param taskId        the id of the task; must not be null
     * @param dependencyIds the ids of tasks this task depends on; must not be null
     */
    void onTaskStart(TaskId taskId, java.util.Collection<TaskId> dependencyIds);

    /**
     * Called when a task completes successfully.
     *
     * @param taskId the id of the task; must not be null
     * @param output the result of execution; may be null
     */
    void onTaskSuccess(TaskId taskId, Object output);

    /**
     * Called when a task completion is skipped.
     *
     * @param taskId the id of the task; must not be null
     */
    void onTaskSkipped(TaskId taskId);

    /**
     * Called when a task fails during execution.
     *
     * @param taskId the id of the task; must not be null
     * @param error  the exception encountered; must not be null
     */
    void onTaskError(TaskId taskId, Throwable error);

    /**
     * Builds the final immutable trace of the entire execution.
     *
     * @return the execution trace; should only be called once execution is complete
     */
    ExecutionTrace build();
}
