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
 * <p>All operations are strictly type-safe, using {@link FlowKey} as the handle
 * for both storage and retrieval.
 */
public interface ReactiveExecutionContext {

    /**
     * Stores a value using a type-safe {@link FlowKey}.
     *
     * @param key   the typed key; must not be null
     * @param value the output value; may be null
     * @param <T>   the value type
     */
    <T> void put(FlowKey<T> key, T value);

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
    <T> Optional<T> get(FlowKey<T> key);

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
     * Returns {@code true} if a value has been stored for the given key.
     *
     * @param key the typed key; must not be null
     * @return {@code true} if the associated task has a stored result
     */
    boolean isCompleted(FlowKey<?> key);

    /**
     * Returns a snapshot of all task ids that have stored results.
     *
     * @return an immutable set of completed task ids
     */
    Set<TaskId> completedTasks();
}