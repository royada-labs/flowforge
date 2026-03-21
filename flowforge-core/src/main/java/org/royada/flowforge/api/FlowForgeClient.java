/*
 * Licensed under the Apache License, Version 2.0
 */
package org.royada.flowforge.api;

import java.time.Duration;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.trace.ExecutionTrace;
import reactor.core.publisher.Mono;

/**
 * Entry point for executing workflows by id.
 */
public interface FlowForgeClient {

    /**
     * Executes a workflow and returns the full execution context.
     *
     * @param workflowId workflow identifier
     * @param input initial input for root tasks
     * @param <T> execution context subtype
     * @return a publisher with the final execution context
     */
    <T extends ReactiveExecutionContext> Mono<T> execute(
            String workflowId,
            Object input);

    /**
     * Executes a workflow with an execution timeout.
     *
     * @param workflowId workflow identifier
     * @param input initial input for root tasks
     * @param timeout timeout applied to execution
     * @return a publisher with the final execution context
     */
    Mono<ReactiveExecutionContext> execute(
            String workflowId,
            Object input,
            Duration timeout);

    /**
     * Executes a workflow and extracts its result based on the structure.
     * Use this for simple workflows where you expect a single output.
         *
         * @param workflowId workflow identifier
         * @param input initial input for root tasks
         * @return a publisher with selected workflow result
     */
    Mono<Object> executeResult(String workflowId, Object input);

    /**
     * Executes a workflow and returns a detailed execution trace.
         *
         * @param workflowId workflow identifier
         * @param input initial input for root tasks
         * @return a publisher with execution trace
     */
    Mono<ExecutionTrace> executeWithTrace(String workflowId, Object input);
}