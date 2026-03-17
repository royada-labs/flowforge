package io.tugrandsolutions.flowforge.workflow;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.tugrandsolutions.flowforge.task.TaskId;

/**
 * Thread-safe store for intermediate task results within a single workflow execution.
 *
 * <p>Results are keyed by {@link TaskId}. The typed {@link FlowKey}-based overloads
 * provide compile-time safety without requiring changes to the underlying storage.
 */
public interface ReactiveExecutionContext {

    // -------------------------------------------------------------------------
    // Core API (TaskId-based) — legacy, remains the source of truth
    // -------------------------------------------------------------------------

    /**
     * Stores a value produced by a task.
     *
     * @param taskId the task identifier; must not be null
     * @param value  the output value; may be null
     * @param <T>    the value type
     */
    <T> void put(TaskId taskId, T value);

    /**
     * Retrieves a value stored by a task, cast to the given type.
     *
     * @param taskId the task identifier; must not be null
     * @param type   the expected value type; must not be null
     * @param <T>    the value type
     * @return an {@link Optional} containing the value if present and type-compatible,
     *         or empty if not found or type does not match
     */
    <T> Optional<T> get(TaskId taskId, Class<T> type);

    /**
     * Retrieves a raw value stored by a task without type casting.
     *
     * @param taskId the task identifier; must not be null
     * @return an {@link Optional} containing the raw value if present
     */
    default Optional<Object> get(TaskId taskId) {
        return get(taskId, Object.class);
    }

    /**
     * Returns {@code true} if a value has been stored for the given task id.
     *
     * @param taskId the task identifier; must not be null
     * @return {@code true} if the task has a stored result
     */
    boolean isCompleted(TaskId taskId);

    /**
     * Returns a snapshot of all task ids that have stored results.
     *
     * @return an immutable set of completed task ids
     */
    Set<TaskId> completedTasks();

    // -------------------------------------------------------------------------
    // Type-safe API (FlowKey-based) — new, delegates to TaskId-based methods
    // -------------------------------------------------------------------------

    /**
     * Stores a value using a type-safe {@link FlowKey}.
     *
     * <p>This is equivalent to {@code put(key.taskId(), value)}.
     *
     * @param key   the typed key; must not be null
     * @param value the output value; may be null
     * @param <T>   the value type
     */
    default <T> void put(FlowKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        put(key.taskId(), value);
    }

    /**
     * Retrieves a value using a type-safe {@link FlowKey}.
     *
     * <p>Unlike the {@link #get(TaskId, Class)} variant, this method throws
     * {@link ClassCastException} if the stored value is not assignable to the
     * key's type, making type mismatches visible immediately (fail-fast).
     *
     * <p>Example:
     * <pre>{@code
     * FlowKey<UserProfile> USER = FlowKey.of("fetchUser", UserProfile.class);
     * UserProfile profile = ctx.get(USER); // compile-time typed, runtime validated
     * }</pre>
     *
     * @param key the typed key; must not be null
     * @param <T> the value type
     * @return an {@link Optional} with the value if present and type-safe,
     *         or empty if not found
     * @throws ClassCastException if the stored value cannot be cast to {@code T}
     */
    default <T> Optional<T> get(FlowKey<T> key) {
        Objects.requireNonNull(key, "key");
        return get(key.taskId(), key.type());
    }

    /**
     * Returns {@code true} if a value has been stored for the given key's task id.
     *
     * @param key the typed key; must not be null
     * @return {@code true} if the associated task has a stored result
     */
    default boolean isCompleted(FlowKey<?> key) {
        Objects.requireNonNull(key, "key");
        return isCompleted(key.taskId());
    }
}