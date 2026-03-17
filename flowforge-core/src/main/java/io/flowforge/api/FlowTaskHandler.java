/*
 * Licensed under the Apache License, Version 2.0
 */
package io.flowforge.api;

import io.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;
import javax.annotation.Nullable;


public interface FlowTaskHandler<I, O> {
    Mono<O> execute(@Nullable I input, ReactiveExecutionContext ctx);
}
