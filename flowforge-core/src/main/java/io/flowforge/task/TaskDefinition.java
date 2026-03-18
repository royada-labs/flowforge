/*
 * Licensed under the Apache License, Version 2.0
 */
package io.flowforge.task;



import java.util.Objects;

/**
 * A compile-time-typed definition of a workflow task, declaring both its
 * expected input type {@code I} and output type {@code O}.
 *
 * <p>Use {@code TaskDefinition} in the typed DSL to enable type-propagation
 * between tasks. When a task's output feeds into another task's input, the
 * compiler verifies type compatibility and the runtime validates it fail-fast.
 *
 * <p>Example:
 * <pre>{@code
 * // Declaration (often as static factory method in a constants class):
 * public static TaskDefinition<Void, UserProfile> fetchUser() {
 *     return TaskDefinition.of("fetchUser", Void.class, UserProfile.class);
 * }
 *
 * // Usage in the typed DSL:
 * builder.startTyped(fetchUser())
 *        .then(enrichUser())
 *        .build();
 * }</pre>
 *
 * <p>This class is immutable and safe for use as a static constant.
 *
 * @param <I> the expected input type for this task
 * @param <O> the expected output type for this task
 */
public final class TaskDefinition<I, O> {

    private final TaskId id;
    private final Class<I> inputType;
    private final Class<O> outputType;

    private TaskDefinition(TaskId id, Class<I> inputType, Class<O> outputType) {
        this.id = Objects.requireNonNull(id, "id");
        this.inputType = Objects.requireNonNull(inputType, "inputType");
        this.outputType = Objects.requireNonNull(outputType, "outputType");
    }

    /**
     * Creates a {@code TaskDefinition} with the given id and type contract.
     *
     * @param id         the task identifier string; must not be null or blank
     * @param inputType  the expected input type; must not be null
     * @param outputType the expected output type; must not be null
     * @param <I>        the input type
     * @param <O>        the output type
     * @return a new {@code TaskDefinition} instance
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public static <I, O> TaskDefinition<I, O> of(String id, Class<I> inputType, Class<O> outputType) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("TaskDefinition id must not be blank");
        }
        return new TaskDefinition<>(TaskId.of(id), inputType, outputType);
    }

    /**
     * Creates a {@code TaskDefinition} from an existing {@link TaskId}.
     *
     * @param taskId     the task identifier; must not be null
     * @param inputType  the expected input type; must not be null
     * @param outputType the expected output type; must not be null
     * @param <I>        the input type
     * @param <O>        the output type
     * @return a new {@code TaskDefinition} instance
     */
    public static <I, O> TaskDefinition<I, O> of(TaskId taskId, Class<I> inputType, Class<O> outputType) {
        return new TaskDefinition<>(taskId, inputType, outputType);
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
     * Returns the raw string identifier of this task.
     * Convenience method equivalent to {@code id().getValue()}.
     *
     * @return the task id string value
     */
    public String idValue() {
        return id.getValue();
    }

    /**
     * Returns the expected input type of this task.
     *
     * @return the input type class
     */
    public Class<I> inputType() {
        return inputType;
    }

    /**
     * Returns the expected output type of this task.
     *
     * @return the output type class
     */
    public Class<O> outputType() {
        return outputType;
    }

    /**
     * Derives a {@link FlowKey} for accessing this task's output from an
     * {@link io.flowforge.workflow.ReactiveExecutionContext}.
     *
     * @return a new {@code FlowKey<O>} for this task's id and output type
     */
    public FlowKey<O> outputKey() {
        return new FlowKey<>(id, outputType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDefinition<?, ?> other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(inputType, other.inputType)
                && Objects.equals(outputType, other.outputType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, inputType, outputType);
    }

    @Override
    public String toString() {
        return "TaskDefinition[id=" + id.getValue()
                + ", input=" + inputType.getSimpleName()
                + ", output=" + outputType.getSimpleName() + "]";
    }
}
