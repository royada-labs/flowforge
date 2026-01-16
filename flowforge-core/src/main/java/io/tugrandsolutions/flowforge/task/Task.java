package io.tugrandsolutions.flowforge.task;

import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface Task<I, O> {

    TaskId id();

    default Set<TaskId> dependencies() {
        return Set.of();
    }

    default boolean optional() {
        return false;
    }

    Mono<O> execute(I input, ReactiveExecutionContext context);
}
