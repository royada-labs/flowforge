/*
 * Licensed under the Apache License, Version 2.0
 */
package org.royada.flowforge.spring.dsl;

import org.royada.flowforge.task.TaskDefinition;

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
     *
     * @param task the first task in the workflow
     * @param <I> input type of the first task
     * @param <O> output type of the first task
     * @return a typed builder chained from {@code task}
     */
    default <I, O> TypedFlowBuilder<O> start(TaskDefinition<I, O> task) {
        return startTyped(task);
    }

    /**
     * Starts a workflow from a typed method reference pointing to a {@code @FlowTask} bean method.
     *
     * @param methodRef serializable method reference to a bean method
     * @param <B> bean type that declares the method
     * @param <I> input type of the referenced task
     * @param <O> output type of the referenced task
     * @return a typed builder chained from the referenced task
     */
    <B, I, O> TypedFlowBuilder<O> start(TaskMethodRef<B, I, O> methodRef);

    /**
     * Starts a workflow from a typed method reference to a {@code @TaskHandler} method.
     *
     * @param methodRef serializable method reference to a handler method
     * @param <B> bean type that declares the method
     * @param <I> input type of the referenced task
     * @param <O> output type of the referenced task
     * @return a typed builder chained from the referenced task
     */
    <B, I, O> TypedFlowBuilder<O> start(TaskCallRef<B, I, O> methodRef);

    /**
     * Starts a workflow from a typed method reference to a {@code @TaskHandler} method
     * that does not declare {@code ReactiveExecutionContext}.
     *
     * @param methodRef serializable method reference to a handler method without context parameter
     * @param <B> bean type that declares the method
     * @param <I> input type of the referenced task
     * @param <O> output type of the referenced task
     * @return a typed builder chained from the referenced task
     */
    <B, I, O> TypedFlowBuilder<O> start(TaskCallNoContextRef<B, I, O> methodRef);

    /**
     * Ultra-fluent alias for method-reference-based starts.
     *
     * @param methodRef serializable method reference to a bean method
     * @param <B> bean type that declares the method
     * @param <I> input type of the referenced task
     * @param <O> output type of the referenced task
     * @return a typed builder chained from the referenced task
     */
    default <B, I, O> TypedFlowBuilder<O> flow(TaskMethodRef<B, I, O> methodRef) {
        return start(methodRef);
    }

    /**
     * Ultra-fluent alias for {@link #start(TaskCallRef)}.
     *
     * @param methodRef serializable method reference to a handler method
     * @param <B> bean type that declares the method
     * @param <I> input type of the referenced task
     * @param <O> output type of the referenced task
     * @return a typed builder chained from the referenced task
     */
    default <B, I, O> TypedFlowBuilder<O> flow(TaskCallRef<B, I, O> methodRef) {
        return start(methodRef);
    }

    /**
     * Ultra-fluent alias for {@link #start(TaskCallNoContextRef)}.
     *
     * @param methodRef serializable method reference to a handler method without context parameter
     * @param <B> bean type that declares the method
     * @param <I> input type of the referenced task
     * @param <O> output type of the referenced task
     * @return a typed builder chained from the referenced task
     */
    default <B, I, O> TypedFlowBuilder<O> flow(TaskCallNoContextRef<B, I, O> methodRef) {
        return start(methodRef);
    }
}
