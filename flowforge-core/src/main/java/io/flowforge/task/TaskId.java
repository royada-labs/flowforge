package io.flowforge.task;

import java.util.Objects;

public final class TaskId {

    private final String value;

    private TaskId(String value) {
        this.value = value;
    }

    public static TaskId of(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TaskId value must not be blank");
        }
        return new TaskId(value);
    }

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
