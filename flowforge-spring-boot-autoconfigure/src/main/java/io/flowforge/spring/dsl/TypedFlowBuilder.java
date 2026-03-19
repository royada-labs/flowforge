package io.flowforge.spring.dsl;

import java.util.Objects;
import java.util.function.Consumer;

import io.flowforge.spring.dsl.internal.TaskReferenceResolver;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

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
    private final TaskDefinition<?, O> tailTask;
    private final TaskReferenceResolver referenceResolver;

    TypedFlowBuilder(FlowBuilder builder, TaskDefinition<?, O> tailTask, TaskReferenceResolver referenceResolver) {
        this.builder = Objects.requireNonNull(builder, "builder");
        this.tailTask = Objects.requireNonNull(tailTask, "tailTask");
        this.referenceResolver = Objects.requireNonNull(referenceResolver, "referenceResolver");
    }

    /**
     * Chains a new task that expects the output of the current tail as its input.
     *
     * @param nextTask the definition of the next task
     * @param <NextO>  the output type of the next task
     * @return a new {@code TypedFlowBuilder} for the next output type
     */
    public <NextO> TypedFlowBuilder<NextO> then(TaskDefinition<O, NextO> nextTask) {
        Objects.requireNonNull(nextTask, "nextTask");

        Class<?> expectedInput = nextTask.inputType();
        Class<?> providedOutput = tailTask.outputType();
        if (!expectedInput.isAssignableFrom(providedOutput)) {
            throw new IllegalArgumentException(
                    "Type mismatch in workflow definition: task '" + nextTask.idValue()
                            + "' expects input type " + expectedInput.getName()
                            + " but received output type " + providedOutput.getName()
                            + " from task '" + tailTask.idValue() + "'"
            );
        }

        return builder.then(nextTask);
    }

    public <B, NextO> TypedFlowBuilder<NextO> then(TaskMethodRef<B, O, NextO> methodRef) {
        return then(referenceResolver.resolve(methodRef));
    }

    public <B, NextO> TypedFlowBuilder<NextO> then(TaskCallRef<B, O, NextO> methodRef) {
        return then(referenceResolver.resolve(methodRef));
    }

    public <B, NextO> TypedFlowBuilder<NextO> then(TaskCallNoContextRef<B, O, NextO> methodRef) {
        return then(referenceResolver.resolve(methodRef));
    }

    /**
     * Joins previous parallel branches into a single task.
     *
     * @param joinTask the definition of the join task
     * @param <NextO>  the output type of the join task
     * @return a new {@code TypedFlowBuilder} for the joined output
     */
    public <NextO> TypedFlowBuilder<NextO> join(TaskDefinition<O, NextO> joinTask) {
        return builder.join(joinTask);
    }

    public <B, NextO> TypedFlowBuilder<NextO> join(TaskMethodRef<B, O, NextO> methodRef) {
        return join(referenceResolver.resolve(methodRef));
    }

    public <B, NextO> TypedFlowBuilder<NextO> join(TaskCallRef<B, O, NextO> methodRef) {
        return join(referenceResolver.resolve(methodRef));
    }

    public <B, NextO> TypedFlowBuilder<NextO> join(TaskCallNoContextRef<B, O, NextO> methodRef) {
        return join(referenceResolver.resolve(methodRef));
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

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <B, X> TypedFlowBuilder<O> parallel(TaskMethodRef<B, O, X>... branches) {
        Consumer<FlowBranch>[] consumers = new Consumer[branches.length];
        for (int i = 0; i < branches.length; i++) {
            TaskDefinition<O, X> task = referenceResolver.resolve(branches[i]);
            consumers[i] = b -> b.then(cast(task));
        }
        return fork(consumers);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <B, X> TypedFlowBuilder<O> parallel(TaskCallRef<B, O, X>... branches) {
        Consumer<FlowBranch>[] consumers = new Consumer[branches.length];
        for (int i = 0; i < branches.length; i++) {
            TaskDefinition<O, X> task = referenceResolver.resolve(branches[i]);
            consumers[i] = b -> b.then(cast(task));
        }
        return fork(consumers);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <B, X> TypedFlowBuilder<O> parallel(TaskCallNoContextRef<B, O, X>... branches) {
        Consumer<FlowBranch>[] consumers = new Consumer[branches.length];
        for (int i = 0; i < branches.length; i++) {
            TaskDefinition<O, X> task = referenceResolver.resolve(branches[i]);
            consumers[i] = b -> b.then(cast(task));
        }
        return fork(consumers);
    }

    /**
     * Finalizes the workflow and builds the execution plan.
     *
     * @return the constructed plan
     */
    public WorkflowExecutionPlan build() {
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <I, X> TaskDefinition<I, X> cast(TaskDefinition<I, ?> task) {
        return (TaskDefinition<I, X>) task;
    }
}
