package org.royada.flowforge.task;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Runtime task contract executed by the orchestrator.
 *
 * @param <I> input type
 * @param <O> output type
 */
public interface Task<I, O> {

    /**
     * Returns the unique task identifier.
     * 
     * @return task identifier
     */
    TaskId id();

    /**
     * Returns the expected input type for this task.
     * 
     * @return task input type
     */
    Class<I> inputType();

    /**
     * Returns the expected output type for this task.
     * 
     * @return task output type
     */
    Class<O> outputType();

    /**
     * Returns the set of task IDs that this task depends on.
     * 
     * @return upstream task dependencies
     */
    default Set<TaskId> dependencies() {
        return Set.of();
    }

    /**
     * Returns whether this task is optional. If true, failure will not stop
     * the workflow execution; downstream tasks depending on it will be skipped.
     * 
     * @return whether task failure is optional/skippable
     */
    default boolean optional() {
        return false;
    }

    /**
     * Executes task logic.
     *
     * @param input task input
     * @param context execution context
     * @return output publisher
     */
    Mono<O> execute(I input, ReactiveExecutionContext context);

    /**
     * Returns a typed key used to store and retrieve the task's output in
     * the execution context.
     * 
     * @return typed output key
     */
    default FlowKey<O> outputKey() {
        return new FlowKey<>(id(), outputType());
    }
}

