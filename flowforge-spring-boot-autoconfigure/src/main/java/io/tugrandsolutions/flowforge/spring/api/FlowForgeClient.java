package io.tugrandsolutions.flowforge.spring.api;

import java.time.Duration;

import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

public interface FlowForgeClient {

        Mono<ReactiveExecutionContext> execute(
                        String workflowId,
                        Object input);

        Mono<ReactiveExecutionContext> execute(
                        String workflowId,
                        Object input,
                        Duration timeout);

        /**
         * Executes a workflow and extracts its result based on the structure.
         * Use this for simple workflows where you expect a single output.
         */
        Mono<Object> executeResult(String workflowId, Object input);
}