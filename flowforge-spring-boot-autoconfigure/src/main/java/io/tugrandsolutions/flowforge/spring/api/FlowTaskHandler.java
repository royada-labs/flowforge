package io.tugrandsolutions.flowforge.spring.api;

import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public interface FlowTaskHandler<I, O> {
    Mono<O> execute(@Nullable I input, ReactiveExecutionContext ctx);
}
