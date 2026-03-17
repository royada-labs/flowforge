package io.flowforge.spring.dsl;

import io.flowforge.dsl.TypedTaskNode;
import io.flowforge.spring.dsl.internal.FlowGraph;
import io.flowforge.spring.dsl.internal.FlowPlanMaterializer;
import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.task.TaskDefinition;

import java.util.Objects;

public final class DefaultFlowDsl implements FlowDsl {

    private final TaskHandlerRegistry taskRegistry;
    private final FlowPlanMaterializer materializer;

    public DefaultFlowDsl(TaskHandlerRegistry taskRegistry) {
        this.taskRegistry = Objects.requireNonNull(taskRegistry, "taskRegistry");
        this.materializer = new FlowPlanMaterializer(taskRegistry);
    }

    @Override
    public FlowBuilder start(String taskId) {
        Objects.requireNonNull(taskId, "taskId");
        FlowGraph graph = FlowGraph.start(taskId);
        return new DefaultFlowBuilder(graph, materializer);
    }

    @Override
    public <I, O> TypedFlowBuilder<O> startTyped(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        FlowGraph graph = FlowGraph.start(task.idValue());
        graph.registerTypeMetadata(task.idValue(), task.inputType(), task.outputType());
        FlowBuilder builder = new DefaultFlowBuilder(graph, materializer);
        TypedTaskNode<O> node = new TypedTaskNode<>(task.toRef());
        return new TypedFlowBuilder<>(builder, node);
    }
}
