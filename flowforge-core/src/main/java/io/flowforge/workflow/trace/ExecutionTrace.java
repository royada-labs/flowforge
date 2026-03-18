package io.flowforge.workflow.trace;

import java.util.List;
import java.util.Objects;

/**
 * Complete trace of a workflow execution.
 *
 * <p>Provides JSON and human-readable string exports for debugging.
 */
public final class ExecutionTrace {
    private final List<TaskExecutionTrace> tasks;
    private final long startTime;
    private final long endTime;
    private final String traceId;

    /**
     * Creates a new execution trace.
     *
     * @param tasks     list of task traces; must not be null
     * @param startTime epoch timestamp of workflow start
     * @param endTime   epoch timestamp of workflow completion/failure
     */
    public ExecutionTrace(List<TaskExecutionTrace> tasks, long startTime, long endTime) {
        this(tasks, startTime, endTime, "");
    }

    /**
     * Creates a new execution trace.
     *
     * @param tasks     list of task traces; must not be null
     * @param startTime epoch timestamp of workflow start
     * @param endTime   epoch timestamp of workflow completion/failure
     * @param traceId   unique identifier for distributed tracing; must not be null
     */
    public ExecutionTrace(List<TaskExecutionTrace> tasks, long startTime, long endTime, String traceId) {
        this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
        this.startTime = startTime;
        this.endTime = endTime;
        this.traceId = Objects.requireNonNull(traceId != null ? traceId : "", "traceId must be non-null");
    }

    public List<TaskExecutionTrace> tasks() { return tasks; }
    public long startTime() { return startTime; }
    public long endTime() { return endTime; }
    public String traceId() { return traceId; }
    public long durationMs() { return endTime - startTime; }

    /**
     * Exports the trace to a JSON string.
     *
     * @return the JSON representation
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"startTime\": ").append(startTime).append(",\n");
        sb.append("  \"endTime\": ").append(endTime).append(",\n");
        sb.append("  \"traceId\": \"").append(traceId).append("\",\n");
        sb.append("  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            TaskExecutionTrace task = tasks.get(i);
            sb.append("    {\n");
            sb.append("      \"taskId\": \"").append(task.taskId()).append("\",\n");
            sb.append("      \"status\": \"").append(task.status()).append("\",\n");
            sb.append("      \"durationMs\": ").append(task.durationMs()).append(",\n");
            sb.append("      \"threadName\": \"").append(task.threadName()).append("\",\n");
            sb.append("      \"inputType\": \"").append(task.inputType()).append("\",\n");
            sb.append("      \"outputType\": \"").append(task.outputType()).append("\"");
            if (task.errorMessage() != null) {
                sb.append(",\n      \"errorMessage\": \"")
                        .append(task.errorMessage().replace("\"", "\\\"")).append("\"");
            }
            sb.append("\n    }").append(i < tasks.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Exports the trace to a human-readable string for console output.
     *
     * @return the pretty string
     */
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Flow Execution Trace\n");
        sb.append("--------------------\n");
        sb.append("Trace ID: ").append(traceId).append("\n");
        for (TaskExecutionTrace task : tasks) {
            sb.append(String.format("%-20s %-10s %5dms",
                    task.taskId(), task.status(), task.durationMs()));
            if (task.errorMessage() != null) {
                sb.append(" (").append(task.errorMessage()).append(")");
            }
            sb.append("\n");
        }
        sb.append("--------------------\n");
        sb.append("Total duration: ").append(durationMs()).append("ms\n");
        return sb.toString();
    }
}
