package io.flowforge.workflow.trace;

import io.flowforge.validation.TypeMetadata;
import java.util.Map;

/**
 * Factory for creating {@link ExecutionTracer} instances.
 * Since tracers are stateful per execution, a new instance must be created for each run.
 */
@FunctionalInterface
public interface ExecutionTracerFactory {
    /**
     * Creates a new tracer instance.
     *
     * @param typeMetadata type information collected from the DSL
     * @return a new tracer
     */
    ExecutionTracer create(Map<String, TypeMetadata> typeMetadata);
}
