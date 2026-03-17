package io.flowforge.spring.dsl;

import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

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
    private final TaskDefinition<?, O> tailTask;

    TypedFlowBuilder(FlowBuilder builder, TaskDefinition<?, O> tailTask) {
        this.builder = Objects.requireNonNull(builder, "builder");
        this.tailTask = Objects.requireNonNull(tailTask, "tailTask");
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
     * Finalizes the workflow and builds the execution plan.
     *
     * @return the constructed plan
     */
    public WorkflowExecutionPlan build() {
        return builder.build();
    }
}
