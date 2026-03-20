package org.royada.flowforge.validation;

import java.util.Objects;

/**
 * Describes a single validation problem found in a workflow definition.
 *
 * <p>Each error has a machine-readable {@link #code()}, a human-readable
 * {@link #message()}, an associated {@link #taskId()} (may be empty for
 * graph-level issues), and a {@link #severity()}.
 *
 * <p>The {@link #formatted()} method produces compiler-style output:
 * <pre>{@code
 * [TYPE_MISMATCH] Task 'enrichUser'
 *   expected: UserProfile
 *   actual:   Order (from 'fetchOrder')
 * }</pre>
 */
public final class FlowValidationError {

    /**
     * Severity level of a validation error.
     */
    public enum Severity {
        /** Blocks execution — the workflow cannot be built. */
        ERROR,
        /** Informational — the workflow can still be built. */
        WARNING
    }

    /** Type mismatch between a task's expected input and its upstream output. */
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    /** Task expects non-Void input but has no upstream dependency. */
    public static final String MISSING_INPUT = "MISSING_INPUT";
    /** Node is not reachable from any root in the DAG. */
    public static final String UNREACHABLE_NODE = "UNREACHABLE_NODE";
    /** Two or more tasks share the same identifier. */
    public static final String DUPLICATE_TASK_ID = "DUPLICATE_TASK_ID";
    /** Task's output is never consumed by a downstream task. */
    public static final String UNUSED_OUTPUT = "UNUSED_OUTPUT";

    private final String code;
    private final String message;
    private final String taskId;
    private final Severity severity;

    /**
     * Creates a validation error.
     *
     * @param code     machine-readable code; must not be null
     * @param message  human-readable description; must not be null
     * @param taskId   the related task id; may be empty for graph-level errors
     * @param severity the severity level; must not be null
     */
    public FlowValidationError(String code, String message, String taskId, Severity severity) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.severity = Objects.requireNonNull(severity, "severity");
    }

    /** Creates an ERROR-severity validation error. */
    public static FlowValidationError error(String code, String taskId, String message) {
        return new FlowValidationError(code, message, taskId, Severity.ERROR);
    }

    /** Creates a WARNING-severity validation error. */
    public static FlowValidationError warning(String code, String taskId, String message) {
        return new FlowValidationError(code, message, taskId, Severity.WARNING);
    }

    public String code() { return code; }
    public String message() { return message; }
    public String taskId() { return taskId; }
    public Severity severity() { return severity; }

    /**
     * Returns a compiler-style formatted representation:
     * <pre>{@code
     * [TYPE_MISMATCH] Task 'enrichUser': expected UserProfile but got Order
     * }</pre>
     *
     * @return the formatted error string
     */
    public String formatted() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(code).append(']');
        if (!taskId.isEmpty()) {
            sb.append(" Task '").append(taskId).append("'");
        }
        sb.append(": ").append(message);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowValidationError other)) return false;
        return Objects.equals(code, other.code)
                && Objects.equals(taskId, other.taskId)
                && severity == other.severity
                && Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, taskId, severity, message);
    }

    @Override
    public String toString() {
        return formatted();
    }
}
