package org.royada.flowforge.workflow.trace;

/**
 * Final status of a task execution in a trace.
 */
public enum ExecutionStatus {
    /** Task completed successfully. */
    SUCCESS,
    /** Task completed with error. */
    ERROR,
    /** Task was skipped. */
    SKIPPED
}
