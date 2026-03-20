package org.royada.flowforge.spring.dsl;

import org.royada.flowforge.api.FlowTaskHandler;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Serializable method reference to a Spring bean method returning a {@link FlowTaskHandler}.
 *
 * @param <B> bean type
 * @param <I> task input type
 * @param <O> task output type
 */
@FunctionalInterface
public interface TaskMethodRef<B, I, O> extends Function<B, FlowTaskHandler<I, O>>, Serializable {
}

