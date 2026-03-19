package io.flowforge.spring.dsl;

import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Serializable instance-method reference to a {@code @FlowTask} method in a {@code @TaskHandler} bean
 * that does not declare {@code ReactiveExecutionContext}.
 */
@FunctionalInterface
public interface TaskCallNoContextRef<B, I, O> extends Serializable {
    Mono<O> invoke(B bean, I input);
}
