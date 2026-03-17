package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.dsl.TypedTaskNode;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A typed wrapper around {@link FlowBuilder} that tracks the output type of the
 * last added task, enabling seamless and type-safe chaining.
 *
 * <p>Example:
 * <pre>{@code
 * WorkflowExecutionPlan plan = dsl.startTyped(taskA)
 *     .then(taskB)
 *     .then(taskC)
 *     .build();
 * }</pre>
 *
 * @param <O> the output type of the last task in the current chain
 */
public final class TypedFlowBuilder<O> {

    private final FlowBuilder builder;
    private final TypedTaskNode<O> node;

    TypedFlowBuilder(FlowBuilder builder, TypedTaskNode<O> node) {
        this.builder = Objects.requireNonNull(builder, "builder");
        this.node = Objects.requireNonNull(node, "node");
    }

    /**
     * Chains a new task that expects the output of the current tail as its input.
     *
     * @param nextTask the definition of the next task
     * @param <NextO>  the output type of the next task
     * @return a new {@code TypedFlowBuilder} for the next output type
     */
    public <NextO> TypedFlowBuilder<NextO> then(TaskDefinition<O, NextO> nextTask) {
        TypedTaskNode<NextO> nextNode = builder.then(nextTask, node);
        return new TypedFlowBuilder<>(builder, nextNode);
    }

    /**
     * Fork into multiple branches, continuing from the current tail.
     *
     * @param branches the branches to create
     * @return this builder (maintaining current output type)
     */
    @SafeVarargs
    public final TypedFlowBuilder<O> fork(Consumer<FlowBranch>... branches) {
        builder.fork(branches);
        return this;
    }

    /**
     * Returns the underlying un-typed {@link FlowBuilder}.
     *
     * @return the raw builder
     */
    public FlowBuilder untyped() {
        return builder;
    }

    /**
     * Returns the {@link TypedTaskNode} representing the current tail.
     *
     * @return the last added node
     */
    public TypedTaskNode<O> node() {
        return node;
    }

    /**
     * Finalizes the workflow and builds the execution plan.
     *
     * @return the constructed plan
     */
    public WorkflowExecutionPlan build() {
        return builder.build();
    }
}
