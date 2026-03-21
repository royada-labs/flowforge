/*
 * Licensed under the Apache License, Version 2.0
 */
package org.royada.flowforge.api;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

/**
 * Functional contract for task execution.
 *
 * @param <I> input type
 * @param <O> output type
 */
public interface FlowTaskHandler<I, O> {
    /**
     * Executes task logic.
     *
     * @param input task input
     * @param ctx execution context
     * @return task output publisher
     */
    Mono<O> execute(I input, ReactiveExecutionContext ctx);
}
