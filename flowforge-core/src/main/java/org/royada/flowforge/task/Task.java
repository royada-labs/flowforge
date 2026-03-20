package org.royada.flowforge.task;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface Task<I, O> {

    TaskId id();

    Class<I> inputType();

    Class<O> outputType();

    default Set<TaskId> dependencies() {
        return Set.of();
    }

    default boolean optional() {
        return false;
    }

    Mono<O> execute(I input, ReactiveExecutionContext context);

    default FlowKey<O> outputKey() {
        return new FlowKey<>(id(), outputType());
    }
}

