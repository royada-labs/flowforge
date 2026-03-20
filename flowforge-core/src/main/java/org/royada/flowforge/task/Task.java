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
     * @return task identifier
     */
    TaskId id();

    /**
     * @return task input type
     */
    Class<I> inputType();

    /**
     * @return task output type
     */
    Class<O> outputType();

    /**
     * @return upstream task dependencies
     */
    default Set<TaskId> dependencies() {
        return Set.of();
    }

    /**
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
     * @return typed output key for storing/retrieving task result in context
     */
    default FlowKey<O> outputKey() {
        return new FlowKey<>(id(), outputType());
    }
}

