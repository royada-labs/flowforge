package io.flowforge.spring.dsl;

import io.flowforge.task.TaskDefinition;

/**
 * Builder for a single parallel branch within a {@code fork} operation.
 *
 * <p>Enforces a strictly typed DSL within parallel branches.
 */
public interface FlowBranch {

    /**
     * Adds a sequential task step to this branch.
     *
     * @param task the task definition
     * @param <I>  the input type
     * @param <O>  the output type
     * @return a {@link TypedFlowBuilder}
     */
    <I, O> TypedFlowBuilder<O> then(TaskDefinition<I, O> task);
    <B, I, O> TypedFlowBuilder<O> then(TaskMethodRef<B, I, O> methodRef);
    <B, I, O> TypedFlowBuilder<O> then(TaskCallRef<B, I, O> methodRef);

    /**
     * Starts nested parallel branches.
     *
     * @param branches branch consumers
     * @return this branch
     */
    @SuppressWarnings("unchecked")
    FlowBranch fork(java.util.function.Consumer<FlowBranch>... branches);
}
