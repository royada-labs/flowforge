package io.flowforge.spring.dsl;

import io.flowforge.spring.dsl.internal.FlowGraph;
import io.flowforge.task.TaskDefinition;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public final class DefaultFlowBranch implements FlowBranch {

    private final FlowGraph branchGraph;
    private final FlowBuilder rootBuilder;
    private final FlowBuilder branchBuilder;

    public DefaultFlowBranch(FlowGraph branchGraph, FlowBuilder builder) {
        this.branchGraph = Objects.requireNonNull(branchGraph, "branchGraph");
        this.rootBuilder = Objects.requireNonNull(builder, "builder");
        this.branchBuilder = new BranchFlowBuilder();
    }


    @Override
    public <I, O> TypedFlowBuilder<O> then(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        branchGraph.then(task);
        return new TypedFlowBuilder<>(branchBuilder, task);
    }

    @SafeVarargs
    @Override
    public final FlowBranch fork(Consumer<FlowBranch>... branches) {
        Objects.requireNonNull(branches, "branches");
        if (branches.length == 0) {
            throw new IllegalArgumentException("fork requires at least 1 branch");
        }

        var branchConsumers = Arrays.stream(branches)
                .map(b -> Objects.requireNonNull(b, "branch"))
                .toList();

        branchGraph.fork(branchConsumers, branchBuilder);
        return this;
    }

    private final class BranchFlowBuilder implements FlowBuilder {
        @Override
        public <I, O> TypedFlowBuilder<O> then(TaskDefinition<I, O> task) {
            Objects.requireNonNull(task, "task");
            branchGraph.then(task);
            return new TypedFlowBuilder<>(this, task);
        }

        @SafeVarargs
        @Override
        public final FlowBuilder fork(Consumer<FlowBranch>... branches) {
            Objects.requireNonNull(branches, "branches");
            if (branches.length == 0) {
                throw new IllegalArgumentException("fork requires at least 1 branch");
            }

            var branchConsumers = Arrays.stream(branches)
                    .map(b -> Objects.requireNonNull(b, "branch"))
                    .toList();

            branchGraph.fork(branchConsumers, this);
            return this;
        }

        @Override
        public <I, O> TypedFlowBuilder<O> join(TaskDefinition<I, O> task) {
            Objects.requireNonNull(task, "task");
            branchGraph.join(task);
            return new TypedFlowBuilder<>(this, task);
        }

        @Override
        public io.flowforge.workflow.plan.WorkflowExecutionPlan build() {
            return rootBuilder.build();
        }
    }
}
