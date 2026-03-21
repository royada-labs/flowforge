package org.royada.flowforge.workflow.trace;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.validation.TypeMetadata;
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
    ExecutionTracer create(Map<TaskId, TypeMetadata> typeMetadata);
}
