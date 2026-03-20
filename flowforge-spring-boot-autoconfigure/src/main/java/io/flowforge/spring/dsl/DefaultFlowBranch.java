package io.flowforge.spring.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import io.flowforge.spring.dsl.internal.FlowGraph;
import io.flowforge.spring.dsl.internal.TaskReferenceResolver;
import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.policy.ExecutionPolicy;

public final class DefaultFlowBranch implements FlowBranch {

    private final FlowGraph branchGraph;
    private final FlowBuilder rootBuilder;
    private final FlowBuilder branchBuilder;
    private final TaskReferenceResolver referenceResolver;

    public DefaultFlowBranch(FlowGraph branchGraph, FlowBuilder builder, TaskReferenceResolver referenceResolver) {
        this.branchGraph = Objects.requireNonNull(branchGraph, "branchGraph");
        this.rootBuilder = Objects.requireNonNull(builder, "builder");
        this.referenceResolver = Objects.requireNonNull(referenceResolver, "referenceResolver");
        this.branchBuilder = new BranchFlowBuilder();
    }


    @Override
    public <I, O> TypedFlowBuilder<O> then(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        branchGraph.then(task);
        return new TypedFlowBuilder<>(branchBuilder, task, referenceResolver);
    }

    @Override
    public <B, I, O> TypedFlowBuilder<O> then(TaskMethodRef<B, I, O> methodRef) {
        return then(referenceResolver.resolve(methodRef));
    }

    @Override
    public <B, I, O> TypedFlowBuilder<O> then(TaskCallRef<B, I, O> methodRef) {
        return then(referenceResolver.resolve(methodRef));
    }

    @Override
    public <B, I, O> TypedFlowBuilder<O> then(TaskCallNoContextRef<B, I, O> methodRef) {
        return then(referenceResolver.resolve(methodRef));
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

        branchGraph.fork(branchConsumers, branchBuilder, referenceResolver);
        return this;
    }

    private final class BranchFlowBuilder implements FlowBuilder {
        @Override
        public <I, O> TypedFlowBuilder<O> then(TaskDefinition<I, O> task) {
            Objects.requireNonNull(task, "task");
            branchGraph.then(task);
            return new TypedFlowBuilder<>(this, task, referenceResolver);
        }

        public <B, I, O> TypedFlowBuilder<O> then(TaskMethodRef<B, I, O> methodRef) {
            return then(referenceResolver.resolve(methodRef));
        }

        public <B, I, O> TypedFlowBuilder<O> then(TaskCallRef<B, I, O> methodRef) {
            return then(referenceResolver.resolve(methodRef));
        }

        public <B, I, O> TypedFlowBuilder<O> then(TaskCallNoContextRef<B, I, O> methodRef) {
            return then(referenceResolver.resolve(methodRef));
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

            branchGraph.fork(branchConsumers, this, referenceResolver);
            return this;
        }

        @Override
        public <I, O> TypedFlowBuilder<O> join(TaskDefinition<I, O> task) {
            Objects.requireNonNull(task, "task");
            branchGraph.join(task);
            return new TypedFlowBuilder<>(this, task, referenceResolver);
        }

        @Override
        public void applyPolicy(TaskId taskId, ExecutionPolicy policy) {
            branchGraph.applyPolicy(taskId, policy);
        }

        @Override
        public io.flowforge.workflow.plan.WorkflowExecutionPlan build() {
            return rootBuilder.build();
        }
    }
}
