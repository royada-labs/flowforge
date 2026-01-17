package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowGraph;
import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowPlanMaterializer;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

final class DefaultFlowBuilder implements FlowBuilder {

    private final FlowGraph graph;
    private final FlowPlanMaterializer materializer;

    DefaultFlowBuilder(FlowGraph graph, FlowPlanMaterializer materializer) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    @Override
    public FlowBuilder then(String taskId) {
        graph.then(taskId);
        return this;
    }

    @SafeVarargs
    @Override
    public final FlowBuilder fork(Consumer<FlowBranch>... branches) {
        Objects.requireNonNull(branches, "branches");
        if (branches.length == 0) {
            throw new IllegalArgumentException("fork requires at least 1 branch");
        }

        var branchBuilders = Arrays.stream(branches)
                .map(b -> (Consumer<FlowBranch>) Objects.requireNonNull(b, "branch"))
                .toList();

        graph.fork(branchBuilders);
        return this;
    }

    @Override
    public FlowBuilder join(String taskId) {
        graph.join(taskId);
        return this;
    }

    @Override
    public WorkflowExecutionPlan build() {
        return materializer.materialize(graph);
    }
}