package io.flowforge.task;


import java.util.Objects;

/**
 * A typed reference to a registered workflow task.
 *
 * <p>A {@code TaskRef<T>} is a compile-time-typed handle to a task by its id,
 * carrying the task's expected output type. It can be used in the FlowForge DSL
 * as an alternative to raw string identifiers, and serves as the foundation for
 * future compile-time DAG validation.
 *
 * <p>Example usage:
 * <pre>{@code
 * TaskRef<UserProfile> fetchUser = TaskRef.of("fetchUser", UserProfile.class);
 *
 * dsl.start(fetchUser)
 *    .then(validateUser)
 *    .build();
 * }</pre>
 *
 * <p>This class is immutable and safe for use as a static constant.
 *
 * @param <T> the output type of the referenced task
 */
public final class TaskRef<T> {

    private final TaskId id;
    private final Class<T> outputType;

    private TaskRef(TaskId id, Class<T> outputType) {
        this.id = Objects.requireNonNull(id, "id");
        this.outputType = Objects.requireNonNull(outputType, "outputType");
    }

    /**
     * Creates a {@code TaskRef} for the given task id and output type.
     *
     * @param id         the task identifier string; must not be null or blank
     * @param outputType the expected output type of the task; must not be null
     * @param <T>        the output type
     * @return a new {@code TaskRef} instance
     * @throws NullPointerException     if {@code id} or {@code outputType} is null
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public static <T> TaskRef<T> of(String id, Class<T> outputType) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("TaskRef id must not be blank");
        }
        return new TaskRef<>(TaskId.of(id), outputType);
    }

    /**
     * Creates a {@code TaskRef} from an existing {@link TaskId}.
     *
     * @param taskId     the task identifier; must not be null
     * @param outputType the expected output type; must not be null
     * @param <T>        the output type
     * @return a new {@code TaskRef} instance
     */
    public static <T> TaskRef<T> of(TaskId taskId, Class<T> outputType) {
        return new TaskRef<>(taskId, outputType);
    }

    /**
     * Returns the underlying {@link TaskId}.
     *
     * @return the task id
     */
    public TaskId id() {
        return id;
    }

    /**
     * Returns the expected output type of the referenced task.
     *
     * @return the output type class
     */
    public Class<T> outputType() {
        return outputType;
    }

    /**
     * Returns the raw string identifier of the referenced task.
     * Convenience method equivalent to {@code id().getValue()}.
     *
     * @return the task id string value
     */
    public String idValue() {
        return id.getValue();
    }

    /**
     * Converts this task reference into a {@link FlowKey} for accessing
     * the task's output from an
     * {@link io.flowforge.workflow.ReactiveExecutionContext}.
     *
     * @return a new {@code FlowKey<T>} with this ref's id and output type
     */
    public FlowKey<T> outputKey() {
        return FlowKey.of(id, outputType);
    }

    /**
     * @deprecated Use {@link #outputKey()} instead.
     */
    @Deprecated(since = "0.4.0", forRemoval = true)
    public FlowKey<T> toKey() {
        return outputKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskRef<?> other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(outputType, other.outputType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, outputType);
    }

    @Override
    public String toString() {
        return "TaskRef[id=" + id.getValue() + ", outputType=" + outputType.getSimpleName() + "]";
    }
}
