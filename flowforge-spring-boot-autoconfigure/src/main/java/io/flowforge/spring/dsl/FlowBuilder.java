package io.flowforge.spring.dsl;

import java.util.function.Consumer;

import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.policy.ExecutionPolicy;

/**
 * Fluent builder for constructing a workflow execution plan.
 *
 * <p>Enforces a strictly typed DSL where every task is defined via {@link TaskDefinition}.
 */
public interface FlowBuilder {

    /**
     * Adds a sequential task step to the workflow.
     *
     * @param task the task definition
     * @param <I>  the input type
     * @param <O>  the output type
     * @return a {@link TypedFlowBuilder} representing this task's output
     */
    <I, O> TypedFlowBuilder<O> then(TaskDefinition<I, O> task);

    /**
     * Starts parallel branches from the current point in the workflow.
     *
     * @param branches consumers that define each parallel branch
     * @return this builder to continue the main flow after the fork
     */
    @SuppressWarnings("unchecked")
    FlowBuilder fork(Consumer<FlowBranch>... branches);

    /**
     * Joins previous parallel branches into a single task.
     *
     * @param task the join task definition
     * @param <I>  the input type
     * @param <O>  the output type
     * @return a {@link TypedFlowBuilder}
     */
    <I, O> TypedFlowBuilder<O> join(TaskDefinition<I, O> task);

    /**
     * Applies an execution policy to an already registered task in the current flow.
     *
     * @param taskId the task id to configure
     * @param policy the policy to apply
     */
    void applyPolicy(TaskId taskId, ExecutionPolicy policy);


    /**
     * Finalizes the workflow definition and produces an executable plan.
     *
     * @return the constructed {@link WorkflowExecutionPlan}
     */
    WorkflowExecutionPlan build();
}
