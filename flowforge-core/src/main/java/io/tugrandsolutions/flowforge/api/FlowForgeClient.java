package io.tugrandsolutions.flowforge.api;

import java.time.Duration;

import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.trace.ExecutionTrace;
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

    /**
     * Executes a workflow and returns a detailed execution trace.
     */
    Mono<ExecutionTrace> executeWithTrace(String workflowId, Object input);
}