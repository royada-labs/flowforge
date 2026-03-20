package org.royada.flowforge.spring.dsl;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import org.royada.flowforge.spring.dsl.internal.TaskReferenceResolver;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.policy.ExecutionPolicy;
import org.royada.flowforge.workflow.policy.RetryPolicy;
import org.royada.flowforge.workflow.policy.TimeoutPolicy;

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

    /**
     * Chains a task using a bean method reference.
     *
     * @param methodRef method reference
     * @param <B> bean type
     * @param <NextO> next output type
     * @return typed builder for the referenced task output
     */
    public <B, NextO> TypedFlowBuilder<NextO> then(TaskMethodRef<B, O, NextO> methodRef) {
        return then(referenceResolver.resolve(methodRef));
    }

    /**
     * Chains a task using a handler method reference with context parameter.
     *
     * @param methodRef method reference
     * @param <B> bean type
     * @param <NextO> next output type
     * @return typed builder for the referenced task output
     */
    public <B, NextO> TypedFlowBuilder<NextO> then(TaskCallRef<B, O, NextO> methodRef) {
        return then(referenceResolver.resolve(methodRef));
    }

    /**
     * Chains a task using a handler method reference without context parameter.
     *
     * @param methodRef method reference
     * @param <B> bean type
     * @param <NextO> next output type
     * @return typed builder for the referenced task output
     */
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

    /**
     * Joins branches using a bean method reference.
     *
     * @param methodRef method reference
     * @param <B> bean type
     * @param <NextO> join output type
     * @return typed builder for the join output
     */
    public <B, NextO> TypedFlowBuilder<NextO> join(TaskMethodRef<B, O, NextO> methodRef) {
        return join(referenceResolver.resolve(methodRef));
    }

    /**
     * Joins branches using a handler method reference with context parameter.
     *
     * @param methodRef method reference
     * @param <B> bean type
     * @param <NextO> join output type
     * @return typed builder for the join output
     */
    public <B, NextO> TypedFlowBuilder<NextO> join(TaskCallRef<B, O, NextO> methodRef) {
        return join(referenceResolver.resolve(methodRef));
    }

    /**
     * Joins branches using a handler method reference without context parameter.
     *
     * @param methodRef method reference
     * @param <B> bean type
     * @param <NextO> join output type
     * @return typed builder for the join output
     */
    public <B, NextO> TypedFlowBuilder<NextO> join(TaskCallNoContextRef<B, O, NextO> methodRef) {
        return join(referenceResolver.resolve(methodRef));
    }

    /**
     * Applies an execution policy to the current tail task.
     *
     * @param policy execution policy
     * @return this builder
     */
    public TypedFlowBuilder<O> withPolicy(ExecutionPolicy policy) {
        builder.applyPolicy(tailTask.id(), Objects.requireNonNull(policy, "policy"));
        return this;
    }

    /**
     * Applies retry policy to the current tail task.
     *
     * @param retryPolicy retry policy
     * @return this builder
     */
    public TypedFlowBuilder<O> withRetry(RetryPolicy retryPolicy) {
        return withPolicy(retryPolicy);
    }

    /**
     * Applies timeout policy to the current tail task.
     *
     * @param timeout timeout duration
     * @return this builder
     */
    public TypedFlowBuilder<O> withTimeout(Duration timeout) {
        return withPolicy(TimeoutPolicy.of(timeout));
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
     * Forks into parallel branches defined via bean method references.
     *
     * @param branches branch method references
     * @param <B> bean type
     * @param <X> branch output type
     * @return this builder
     */
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

    /**
     * Forks into parallel branches defined via handler method references with context parameter.
     *
     * @param branches branch method references
     * @param <B> bean type
     * @param <X> branch output type
     * @return this builder
     */
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

    /**
     * Forks into parallel branches defined via handler method references without context parameter.
     *
     * @param branches branch method references
     * @param <B> bean type
     * @param <X> branch output type
     * @return this builder
     */
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
