/*
 * Licensed under the Apache License, Version 2.0
 */
package io.flowforge.workflow;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.flowforge.task.TaskId;
import io.flowforge.task.FlowKey;
import io.flowforge.exception.TypeMismatchException;
import java.util.NoSuchElementException;

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
     * @deprecated Use {@link #put(FlowKey, Object)} for type safety.
     */
    @Deprecated(since = "0.4.0", forRemoval = false)
    <T> void put(TaskId taskId, T value);

    /**
     * Retrieves a value stored by a task, cast to the given type.
     *
     * @param taskId the task identifier; must not be null
     * @param type   the expected value type; must not be null
     * @param <T>    the value type
     * @return an {@link Optional} containing the value if present and type-compatible,
     *         or empty if not found or type does not match
     * @deprecated Use {@link #get(FlowKey)} for type safety.
     */
    @Deprecated(since = "0.4.0", forRemoval = false)
    <T> Optional<T> get(TaskId taskId, Class<T> type);

    /**
     * Retrieves a raw value stored by a task without type casting.
     *
     * @param taskId the task identifier; must not be null
     * @return an {@link Optional} containing the raw value if present
     * @deprecated Use {@link #get(FlowKey)} for type safety.
     */
    @Deprecated(since = "0.4.0", forRemoval = false)
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
     * <p>This method performs runtime type validation. If a value exists for the
     * associated task but is not compatible with the expected type, it throws
     * {@link TypeMismatchException} (fail-fast).
     *
     * @param key the typed key; must not be null
     * @param <T> the value type
     * @return an {@link Optional} with the value if present, or empty if not found
     * @throws TypeMismatchException if the stored value is not assignable to {@code T}
     */
    default <T> Optional<T> get(FlowKey<T> key) {
        Objects.requireNonNull(key, "key");
        Optional<Object> raw = get(key.taskId());
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        Object value = raw.get();
        if (value != null && !key.type().isInstance(value)) {
            throw new TypeMismatchException(key.taskId(), key.type(), value.getClass());
        }
        @SuppressWarnings("unchecked")
        T casted = (T) value;
        return Optional.ofNullable(casted);
    }

    /**
     * Retrieves a value using a type-safe {@link FlowKey}, throwing an exception if not found.
     *
     * @param key the typed key; must not be null
     * @param <T> the value type
     * @return the stored value
     * @throws NoSuchElementException if no value is found for the given key
     * @throws TypeMismatchException if the stored value is of the wrong type
     */
    default <T> T getOrThrow(FlowKey<T> key) {
        return get(key).orElseThrow(() -> new NoSuchElementException("No value found for task: " + key.taskId().getValue()));
    }

    /**
     * Retrieves a value using a type-safe {@link FlowKey}, returning a default value if not found.
     *
     * @param key          the typed key; must not be null
     * @param defaultValue the value to return if nothing is found
     * @param <T>          the value type
     * @return the stored value or the default value
     * @throws TypeMismatchException if the stored value is of the wrong type
     */
    default <T> T getOrDefault(FlowKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
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