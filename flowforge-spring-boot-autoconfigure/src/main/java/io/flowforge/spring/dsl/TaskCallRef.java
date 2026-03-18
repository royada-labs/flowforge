package io.flowforge.spring.dsl;

import io.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Serializable instance-method reference to a {@code @FlowTask} method in a {@code @TaskHandler} bean.
 */
@FunctionalInterface
public interface TaskCallRef<B, I, O> extends Serializable {
    Mono<O> invoke(B bean, I input, ReactiveExecutionContext ctx);
}

