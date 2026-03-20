package org.royada.flowforge.spring.dsl.internal;

import org.royada.flowforge.spring.dsl.TaskCallNoContextRef;
import org.royada.flowforge.spring.dsl.TaskCallRef;
import org.royada.flowforge.spring.dsl.TaskMethodRef;
import org.royada.flowforge.task.TaskDefinition;

/**
 * Resolves serialized task method references into typed {@link TaskDefinition} metadata.
 */
public interface TaskReferenceResolver {
    /**
     * Resolves a bean-method reference that returns a task handler.
     *
     * @param ref serialized method reference
     * @param <I> input type
     * @param <O> output type
     * @return resolved task definition
     */
    <I, O> TaskDefinition<I, O> resolve(TaskMethodRef<?, I, O> ref);

    /**
     * Resolves a task-handler method reference that receives context.
     *
     * @param ref serialized method reference
     * @param <B> bean type
     * @param <I> input type
     * @param <O> output type
     * @return resolved task definition
     */
    <B, I, O> TaskDefinition<I, O> resolve(TaskCallRef<B, I, O> ref);

    /**
     * Resolves a task-handler method reference that does not declare context.
     *
     * @param ref serialized method reference
     * @param <B> bean type
     * @param <I> input type
     * @param <O> output type
     * @return resolved task definition
     */
    <B, I, O> TaskDefinition<I, O> resolve(TaskCallNoContextRef<B, I, O> ref);
}
