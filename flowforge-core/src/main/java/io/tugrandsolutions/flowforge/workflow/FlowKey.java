package io.tugrandsolutions.flowforge.workflow;

import io.tugrandsolutions.flowforge.task.TaskId;

import java.util.Objects;

/**
 * A type-safe key for reading values from a {@link ReactiveExecutionContext}.
 *
 * <p>A {@code FlowKey<T>} binds a task identifier to the expected output type
 * {@code T}, enabling compile-time type safety when accessing workflow results.
 *
 * <p>Example usage:
 * <pre>{@code
 * FlowKey<UserProfile> USER = FlowKey.of("fetchUser", UserProfile.class);
 *
 * // Inside a task or service:
 * ctx.put(USER, profile);
 * UserProfile p = ctx.get(USER);
 * }</pre>
 *
 * <p>This class is immutable and safe for use as a static constant.
 *
 * @param <T> the type of value associated with this key
 */
public final class FlowKey<T> {

    private final TaskId taskId;
    private final Class<T> type;

    private FlowKey(TaskId taskId, Class<T> type) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a {@code FlowKey} for the given task id and value type.
     *
     * @param id   the task identifier string; must not be null or blank
     * @param type the expected output type of the task; must not be null
     * @param <T>  the value type
     * @return a new {@code FlowKey} instance
     * @throws NullPointerException     if {@code id} or {@code type} is null
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public static <T> FlowKey<T> of(String id, Class<T> type) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("FlowKey id must not be blank");
        }
        return new FlowKey<>(TaskId.of(id), type);
    }

    /**
     * Creates a {@code FlowKey} from an existing {@link TaskId}.
     *
     * @param taskId the task identifier; must not be null
     * @param type   the expected output type; must not be null
     * @param <T>    the value type
     * @return a new {@code FlowKey} instance
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
