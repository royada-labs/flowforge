package org.royada.flowforge.spring.dsl;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * Serializable instance-method reference to a {@code @FlowTask} method in a {@code @TaskHandler} bean.
 *
 * @param <B> bean type
 * @param <I> task input type
 * @param <O> task output type
 */
@FunctionalInterface
public interface TaskCallRef<B, I, O> extends Serializable {
    /**
     * Invokes the referenced handler method.
     *
     * @param bean target bean instance
     * @param input task input value
     * @param ctx execution context
     * @return task result publisher
     */
    Mono<O> invoke(B bean, I input, ReactiveExecutionContext ctx);
}

