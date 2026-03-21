package org.royada.flowforge.workflow.instance;

/**
 * Represents the execution status of a task within a workflow instance.
 */
public enum TaskStatus {
    /** The task is waiting for its dependencies to complete. */
    PENDING,
    /** All dependencies are satisfied, the task is ready for execution. */
    READY,
    /** The task is currently being executed. */
    RUNNING,
    /** The task has completed successfully. */
    COMPLETED,
    /** The task has failed. */
    FAILED,
    /** The task was skipped (e.g., because it's optional and a dependency failed). */
    SKIPPED
}