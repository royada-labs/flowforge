package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.dsl.TypedTaskNode;
import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowGraph;
import io.tugrandsolutions.flowforge.task.TaskDefinition;

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

    @Override
    public <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        branchGraph.then(task.idValue());
        branchGraph.registerTypeMetadata(task.idValue(), task.inputType(), task.outputType());
        return new TypedTaskNode<>(task.toRef());
    }

    @Override
    public <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task, TypedTaskNode<I> input) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(input, "input");

        Class<?> expectedInput = task.inputType();
        Class<?> providedOutput = input.ref().outputType();
        if (!expectedInput.isAssignableFrom(providedOutput)) {
            throw new IllegalArgumentException(
                    "Type mismatch: task '" + task.idValue()
                            + "' expects " + expectedInput.getName()
                            + " but got " + providedOutput.getName()
                            + " from '" + input.ref().idValue() + "'"
            );
        }

        branchGraph.then(task.idValue());
        branchGraph.registerTypeMetadata(task.idValue(), task.inputType(), task.outputType());
        return new TypedTaskNode<>(task.toRef());
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