package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.dsl.TypedTaskNode;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.task.TaskRef;

/**
 * Builder for a single parallel branch within a {@code fork} operation.
 *
 * <p>Both string-based and typed {@link TaskRef}-based overloads are supported.
 */
public interface FlowBranch {

    /**
     * Adds a sequential task step to this branch.
     *
     * @param taskId the task id; must not be null or blank
     * @return this branch
     */
    FlowBranch then(String taskId);

    /**
     * Adds a sequential typed task step to this branch.
     *
     * @param ref the typed task reference; must not be null
     * @param <T> the output type of the task
     * @return this branch
     */
    default <T> FlowBranch then(TaskRef<T> ref) {
        java.util.Objects.requireNonNull(ref, "ref");
        return then(ref.idValue());
    }

    /**
     * Adds a typed task step to this branch using a {@link TaskDefinition}.
     *
     * @param task the task definition; must not be null
     * @param <I>  the input type
     * @param <O>  the output type
     * @return a {@link TypedTaskNode} representing the task's output
     */
    default <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task) {
        java.util.Objects.requireNonNull(task, "task");
        then(task.idValue());
        return new TypedTaskNode<>(task.toRef());
    }

    /**
     * Adds a typed task step with explicit input validation.
     *
     * @param task  the task definition; must not be null
     * @param input the typed node providing input; must not be null
     * @param <I>   the input type
     * @param <O>   the output type
     * @return a {@link TypedTaskNode} representing the task's output
     * @throws IllegalArgumentException if input type is incompatible
     */
    default <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task, TypedTaskNode<I> input) {
        java.util.Objects.requireNonNull(task, "task");
        java.util.Objects.requireNonNull(input, "input");

        Class<?> expectedInput = task.inputType();
        Class<?> providedOutput = input.ref().outputType();
        if (!expectedInput.isAssignableFrom(providedOutput)) {
            throw new IllegalArgumentException(
                    "Type mismatch: task '" + task.idValue()
                            + "' expects " + expectedInput.getName()
                            + " but got " + providedOutput.getName()
                            + " from '" + input.ref().idValue() + "'"
            );
        }

        then(task.idValue());
        return new TypedTaskNode<>(task.toRef());
    }

    /**
     * Starts nested parallel branches within this branch.
     *
     * @param branches one or more branch consumers; must not be null or empty
     * @return this branch
     */
    FlowBranch fork(java.util.function.Consumer<FlowBranch>... branches);
}