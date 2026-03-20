package org.royada.flowforge.task;

import java.util.Objects;

/**
 * Unique identifier for a task within a workflow.
 */
public final class TaskId {

    private final String value;

    private TaskId(String value) {
        this.value = value;
    }

    /**
     * Creates a new task ID from the given string value.
     * 
     * @param value the string value for the ID
     * @return a new task ID instance
     * @throws NullPointerException if the value is null
     * @throws IllegalArgumentException if the value is blank
     */
    public static TaskId of(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TaskId value must not be blank");
        }
        return new TaskId(value);
    }

    /**
     * Returns the string representation of the ID.
     * 
     * @return the string value
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskId taskId)) return false;
        return value.equals(taskId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "TaskId(value=" + value + ')';
    }
}
