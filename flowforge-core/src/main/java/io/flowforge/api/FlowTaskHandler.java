/*
 * Licensed under the Apache License, Version 2.0
 */
package io.flowforge.api;

import io.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;


public interface FlowTaskHandler<I, O> {
    Mono<O> execute(I input, ReactiveExecutionContext ctx);
}
