package io.tugrandsolutions.flowforge.workflow.trace;

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
     * Called when a task starts execution.
     *
     * @param taskId the id of the task; must not be null
     */
    void onTaskStart(String taskId);

    /**
     * Called when a task completes successfully.
     *
     * @param taskId the id of the task; must not be null
     * @param output the result of execution; may be null
     */
    void onTaskSuccess(String taskId, Object output);

    /**
     * Called when a task completion is skipped.
     *
     * @param taskId the id of the task; must not be null
     */
    void onTaskSkipped(String taskId);

    /**
     * Called when a task fails during execution.
     *
     * @param taskId the id of the task; must not be null
     * @param error  the exception encountered; must not be null
     */
    void onTaskError(String taskId, Throwable error);

    /**
     * Builds the final immutable trace of the entire execution.
     *
     * @return the execution trace; should only be called once execution is complete
     */
    ExecutionTrace build();
}
