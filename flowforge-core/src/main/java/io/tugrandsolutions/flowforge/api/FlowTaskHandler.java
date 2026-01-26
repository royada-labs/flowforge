package io.tugrandsolutions.flowforge.api;

import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;
import javax.annotation.Nullable;


public interface FlowTaskHandler<I, O> {
    Mono<O> execute(@Nullable I input, ReactiveExecutionContext ctx);
}
