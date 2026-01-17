package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowGraph;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public final class DefaultFlowBranch implements FlowBranch {

    private final FlowGraph branchGraph;

    public DefaultFlowBranch(FlowGraph branchGraph) {
        this.branchGraph = Objects.requireNonNull(branchGraph, "branchGraph");
    }

    @Override
    public FlowBranch then(String taskId) {
        branchGraph.then(taskId);
        return this;
    }

    @SafeVarargs
    @Override
    public final FlowBranch fork(Consumer<FlowBranch>... branches) {
        Objects.requireNonNull(branches, "branches");
        if (branches.length == 0) {
            throw new IllegalArgumentException("fork requires at least 1 branch");
        }

        var branchConsumers = Arrays.stream(branches)
                .map(b -> (Consumer<FlowBranch>) Objects.requireNonNull(b, "branch"))
                .toList();

        branchGraph.fork(branchConsumers);
        return this;
    }
}