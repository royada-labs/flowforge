package io.flowforge.spring.dsl;

import io.flowforge.api.FlowTaskHandler;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Serializable method reference to a Spring bean method returning a {@link FlowTaskHandler}.
 */
@FunctionalInterface
public interface TaskMethodRef<B, I, O> extends Function<B, FlowTaskHandler<I, O>>, Serializable {
}

