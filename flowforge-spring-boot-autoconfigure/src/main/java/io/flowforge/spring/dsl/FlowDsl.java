/*
 * Licensed under the Apache License, Version 2.0
 */
package io.flowforge.spring.dsl;

import io.flowforge.task.TaskDefinition;

/**
 * Entry point for defining FlowForge workflows using a fluent DSL.
 */
public interface FlowDsl {
    /**
     * Starts a workflow definition using a fully-typed {@link TaskDefinition},
     * returning a builder tracking the output type for seamless chaining.
     *
     * <p>Example:
     * <pre>{@code
     * WorkflowExecutionPlan plan = dsl.startTyped(fetchUser())
     *     .then(enrichUser())
     *     .build();
     * }</pre>
     *
     * @param task the task definition for the starting task
     * @param <I>  the input type of the starting task
     * @param <O>  the output type of the starting task
     * @return a {@link TypedFlowBuilder} tracking the output type {@code O}
     */
    <I, O> TypedFlowBuilder<O> startTyped(TaskDefinition<I, O> task);

    /**
     * Alias of {@link #startTyped(TaskDefinition)} for concise DSL usage.
     */
    default <I, O> TypedFlowBuilder<O> start(TaskDefinition<I, O> task) {
        return startTyped(task);
    }

    /**
     * Starts a workflow from a typed method reference pointing to a {@code @FlowTask} bean method.
     */
    <B, I, O> TypedFlowBuilder<O> start(TaskMethodRef<B, I, O> methodRef);

    /**
     * Starts a workflow from a typed method reference to a {@code @TaskHandler} method.
     */
    <B, I, O> TypedFlowBuilder<O> start(TaskCallRef<B, I, O> methodRef);

    /**
     * Ultra-fluent alias for method-reference-based starts.
     */
    default <B, I, O> TypedFlowBuilder<O> flow(TaskMethodRef<B, I, O> methodRef) {
        return start(methodRef);
    }

    default <B, I, O> TypedFlowBuilder<O> flow(TaskCallRef<B, I, O> methodRef) {
        return start(methodRef);
    }
}
