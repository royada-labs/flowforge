package org.royada.flowforge.spring.dsl;

import java.util.Objects;

import org.royada.flowforge.spring.dsl.internal.FlowGraph;
import org.royada.flowforge.spring.dsl.internal.FlowPlanMaterializer;
import org.royada.flowforge.spring.dsl.internal.MethodReferenceTaskResolver;
import org.royada.flowforge.spring.dsl.internal.TaskReferenceResolver;
import org.royada.flowforge.spring.registry.TaskDefinitionRegistry;
import org.royada.flowforge.spring.registry.TaskHandlerRegistry;
import org.royada.flowforge.task.TaskDefinition;

/**
 * Default implementation of {@link FlowDsl} backed by the Spring task registries.
 */
public final class DefaultFlowDsl implements FlowDsl {

    private final TaskHandlerRegistry taskRegistry;
    private final FlowPlanMaterializer materializer;
    private final TaskReferenceResolver referenceResolver;

    /**
     * Creates a DSL instance that resolves tasks from Spring-managed registries.
     *
     * @param taskRegistry registry of executable task providers
     * @param definitionRegistry registry of task definitions used by method-reference resolution
     */
    public DefaultFlowDsl(TaskHandlerRegistry taskRegistry, TaskDefinitionRegistry definitionRegistry) {
        this.taskRegistry = Objects.requireNonNull(taskRegistry, "taskRegistry");
        this.materializer = new FlowPlanMaterializer(taskRegistry);
        this.referenceResolver = new MethodReferenceTaskResolver(
                Objects.requireNonNull(definitionRegistry, "definitionRegistry"));
    }

    @Override
    public <I, O> TypedFlowBuilder<O> startTyped(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        FlowGraph graph = FlowGraph.start(task);
        FlowBuilder builder = new DefaultFlowBuilder(graph, materializer, referenceResolver);
        return new TypedFlowBuilder<>(builder, task, referenceResolver);
    }

    @Override
    public <B, I, O> TypedFlowBuilder<O> start(TaskMethodRef<B, I, O> methodRef) {
        TaskDefinition<I, O> task = referenceResolver.resolve(methodRef);
        return startTyped(task);
    }

    @Override
    public <B, I, O> TypedFlowBuilder<O> start(TaskCallRef<B, I, O> methodRef) {
        TaskDefinition<I, O> task = referenceResolver.resolve(methodRef);
        return startTyped(task);
    }

    @Override
    public <B, I, O> TypedFlowBuilder<O> start(TaskCallNoContextRef<B, I, O> methodRef) {
        TaskDefinition<I, O> task = referenceResolver.resolve(methodRef);
        return startTyped(task);
    }
}
