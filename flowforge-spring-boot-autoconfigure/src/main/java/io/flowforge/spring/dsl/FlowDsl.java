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
}
