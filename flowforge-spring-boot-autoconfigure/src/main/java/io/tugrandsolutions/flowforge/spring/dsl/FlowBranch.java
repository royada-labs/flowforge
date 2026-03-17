package io.tugrandsolutions.flowforge.spring.dsl;

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
     * Starts nested parallel branches within this branch.
     *
     * @param branches one or more branch consumers; must not be null or empty
     * @return this branch
     */
    FlowBranch fork(java.util.function.Consumer<FlowBranch>... branches);
}