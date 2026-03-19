package io.flowforge.spring.dsl;

import io.flowforge.spring.dsl.internal.FlowGraph;
import io.flowforge.spring.dsl.internal.MethodReferenceTaskResolver;
import io.flowforge.spring.dsl.internal.FlowPlanMaterializer;
import io.flowforge.spring.dsl.internal.TaskReferenceResolver;
import io.flowforge.spring.registry.TaskDefinitionRegistry;
import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.task.TaskDefinition;

import java.util.Objects;

public final class DefaultFlowDsl implements FlowDsl {

    private final TaskHandlerRegistry taskRegistry;
    private final FlowPlanMaterializer materializer;
    private final TaskReferenceResolver referenceResolver;

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
