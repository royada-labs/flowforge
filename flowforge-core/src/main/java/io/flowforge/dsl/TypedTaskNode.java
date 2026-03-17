package io.flowforge.dsl;

import io.flowforge.task.TaskRef;
import io.flowforge.task.FlowKey;

import java.util.Objects;

/**
 * Represents the typed output of a task within a type-safe DAG definition.
 *
 * <p>A {@code TypedTaskNode<T>} is produced by the typed DSL methods
 * (e.g. {@code builder.then(TaskDefinition)}) and can be passed as an input
 * reference to subsequent tasks, enabling compile-time type propagation.
 *
 * <p>Example:
 * <pre>{@code
 * TypedTaskNode<UserProfile> user = builder.then(fetchUser());
 * TypedTaskNode<EnrichedUser> enriched = builder.then(enrichUser(), user);
 *
 * // Later, read the result from the execution context:
 * UserProfile profile = ctx.get(user.toKey()).orElseThrow();
 * }</pre>
 *
 * <p>This class is immutable.
 *
 * @param <T> the output type of the represented task
 */
public final class TypedTaskNode<T> {

    private final TaskRef<T> ref;

    /**
     * Creates a new {@code TypedTaskNode} wrapping the given task reference.
     *
     * @param ref the typed task reference; must not be null
     */
    public TypedTaskNode(TaskRef<T> ref) {
        this.ref = Objects.requireNonNull(ref, "ref");
    }

    /**
     * Returns the underlying {@link TaskRef}.
     *
     * @return the task reference
     */
    public TaskRef<T> ref() {
        return ref;
    }

    /**
     * Derives a {@link FlowKey} for accessing this task's output from an
     * {@link io.flowforge.workflow.ReactiveExecutionContext}.
     *
     * <p>Convenience shorthand for {@code ref().outputKey()}.
     *
     * @return a {@code FlowKey<T>} for this task's id and output type
     */
    public FlowKey<T> outputKey() {
        return ref.outputKey();
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
        if (!(o instanceof TypedTaskNode<?> other)) return false;
        return Objects.equals(ref, other.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref);
    }

    @Override
    public String toString() {
        return "TypedTaskNode[" + ref + "]";
    }
}
