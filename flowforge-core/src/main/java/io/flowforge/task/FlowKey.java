package io.flowforge.task;

import java.util.Objects;

/**
 * A type-safe key for reading values from an execution context.
 *
 * <p>A {@code FlowKey<T>} binds a task identifier to the expected output type
 * {@code T}, enabling compile-time type safety when accessing workflow results.
 *
 * <p>Creation is restricted to the system (e.g., via {@code TaskDefinition.outputKey()})
 * to ensure consistency between declarations and keys.
 *
 * <p>This class is immutable and safe for use as a static constant.
 *
 * @param <T> the type of value associated with this key
 */
public final class FlowKey<T> {

    private final TaskId taskId;
    private final Class<T> type;

    /**
     * Public constructor for manual instantiation.
     */
    public FlowKey(TaskId taskId, Class<T> type) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Public factory method.
     */
    public static <T> FlowKey<T> of(String id, Class<T> type) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("FlowKey id must not be blank");
        }
        return new FlowKey<>(TaskId.of(id), type);
    }

    /**
     * Public factory method.
     */
    public static <T> FlowKey<T> of(TaskId taskId, Class<T> type) {
        return new FlowKey<>(taskId, type);
    }

    /**
     * Returns the underlying {@link TaskId}.
     *
     * @return the task id
     */
    public TaskId taskId() {
        return taskId;
    }

    /**
     * Returns the expected value type bound to this key.
     *
     * @return the value type class
     */
    public Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowKey<?> other)) return false;
        return Objects.equals(taskId, other.taskId)
                && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, type);
    }

    @Override
    public String toString() {
        return "FlowKey[id=" + taskId.getValue() + ", type=" + type.getSimpleName() + "]";
    }
}
