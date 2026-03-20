package org.royada.flowforge.spring.dsl;

import java.io.Serializable;

import reactor.core.publisher.Mono;

/**
 * Serializable instance-method reference to a {@code @FlowTask} method in a {@code @TaskHandler} bean
 * that does not declare {@code ReactiveExecutionContext}.
 *
 * @param <B> bean type
 * @param <I> task input type
 * @param <O> task output type
 */
@FunctionalInterface
public interface TaskCallNoContextRef<B, I, O> extends Serializable {
    /**
     * Invokes the referenced handler method.
     *
     * @param bean target bean instance
     * @param input task input value
     * @return task result publisher
     */
    Mono<O> invoke(B bean, I input);
}
