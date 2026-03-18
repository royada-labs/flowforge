package io.flowforge.workflow.trace;

/**
 * Detailed trace information for a single task execution.
 *
 * @param taskId       the id of the task
 * @param status       the execution status (SUCCESS, ERROR, SKIPPED)
 * @param startTime    epoch timestamp of execution start
 * @param endTime      epoch timestamp of execution end
 * @param durationMs   duration in milliseconds
 * @param threadName   name of the thread that executed the task
 * @param errorMessage error message if any, otherwise null
 * @param inputType    simple name of the input type if known, otherwise "Unknown"
 * @param outputType   simple name of the output type if known, otherwise "Unknown"
 */
public record TaskExecutionTrace(
        String taskId,
        ExecutionStatus status,
        long startTime,
        long endTime,
        long durationMs,
        String threadName,
        String errorMessage,
        String inputType,
        String outputType
) {
}
