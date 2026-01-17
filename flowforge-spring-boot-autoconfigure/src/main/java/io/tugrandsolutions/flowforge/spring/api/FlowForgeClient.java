package io.tugrandsolutions.flowforge.spring.api;

import reactor.core.publisher.Mono;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;

import java.time.Duration;

public interface FlowForgeClient {

    Mono<ReactiveExecutionContext> execute(
            String workflowId,
            Object input
    );

    Mono<ReactiveExecutionContext> execute(
            String workflowId,
            Object input,
            Duration timeout
    );
}