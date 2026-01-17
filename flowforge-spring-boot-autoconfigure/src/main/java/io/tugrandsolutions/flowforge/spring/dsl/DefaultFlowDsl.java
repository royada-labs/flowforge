package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowGraph;
import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowPlanMaterializer;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;

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
}
