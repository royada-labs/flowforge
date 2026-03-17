package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.dsl.TypedTaskNode;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.task.TaskRef;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Fluent builder for constructing a workflow execution plan.
 *
 * <p>Three levels of type safety are available:
 * <ol>
 *   <li><b>String-based</b> — {@code then("taskId")} — no compile-time safety</li>
 *   <li><b>TaskRef-based</b> — {@code then(TaskRef<T>)} — output type only</li>
 *   <li><b>TaskDefinition-based</b> — {@code then(TaskDefinition<I,O>)} — full I/O
 *       contract with compile-time type propagation</li>
 * </ol>
 *
 * <p>All overloads coexist and can be mixed freely within a workflow definition.
 */
public interface FlowBuilder {

    // -------------------------------------------------------------------------
    // Level 1: String-based (legacy)
    // -------------------------------------------------------------------------

    /**
     * Chains the given task sequentially after the current tail(s).
     *
     * @param taskId the id of the task; must not be null or blank
     * @return this builder
     */
    FlowBuilder then(String taskId);

    // -------------------------------------------------------------------------
    // Level 2: TaskRef-based (output-typed)
    // -------------------------------------------------------------------------

    /**
     * Chains the given typed task reference sequentially after the current tail(s).
     *
     * @param ref the typed reference to the task; must not be null
     * @param <T> the output type of the task
     * @return this builder
     */
    default <T> FlowBuilder then(TaskRef<T> ref) {
        java.util.Objects.requireNonNull(ref, "ref");
        return then(ref.idValue());
    }

    // -------------------------------------------------------------------------
    // Level 3: TaskDefinition-based (full I/O typed with propagation)
    // -------------------------------------------------------------------------

    /**
     * Adds a task to the workflow and returns a typed node representing its output.
     *
     * <p>The returned {@link TypedTaskNode} can be passed to subsequent
     * {@code then(TaskDefinition, TypedTaskNode)} calls to establish type-safe
     * data flow between tasks.
     *
     * <p>This overload does <b>not</b> validate input types; the task will receive
     * whatever the orchestrator's input resolver provides (typically the output
     * of the preceding task in the DAG).
     *
     * @param task the task definition containing id and type contract
     * @param <I>  the input type of the task (not validated in this overload)
     * @param <O>  the output type of the task
     * @return a {@link TypedTaskNode} representing this task's output in the DAG
     */
    default <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task) {
        java.util.Objects.requireNonNull(task, "task");
        then(task.idValue());
        return new TypedTaskNode<>(task.toRef());
    }

    /**
     * Adds a task to the workflow with an explicit typed input, validating type
     * compatibility at definition time (fail-fast).
     *
     * <p>If the input node's output type is not assignable to the task's input
     * type, an {@link IllegalArgumentException} is thrown immediately, before
     * the workflow is materialized or executed.
     *
     * <p>Example:
     * <pre>{@code
     * TypedTaskNode<UserProfile> user = builder.then(fetchUser());
     * TypedTaskNode<EnrichedUser> enriched = builder.then(enrichUser(), user);
     * // compile-time: fetchUser().outputType feeds into enrichUser().inputType
     * // runtime: validates UserProfile.class.isAssignableFrom(UserProfile.class)
     * }</pre>
     *
     * @param task  the task definition containing id and type contract
     * @param input the typed node whose output feeds this task's input
     * @param <I>   the input type of the task — must match input node's output
     * @param <O>   the output type of the task
     * @return a {@link TypedTaskNode} representing this task's output in the DAG
     * @throws IllegalArgumentException if the input type is not compatible
     */
    default <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task, TypedTaskNode<I> input) {
        java.util.Objects.requireNonNull(task, "task");
        java.util.Objects.requireNonNull(input, "input");

        Class<?> expectedInput = task.inputType();
        Class<?> providedOutput = input.ref().outputType();

        if (!expectedInput.isAssignableFrom(providedOutput)) {
            throw new IllegalArgumentException(
                    "Type mismatch in workflow definition: task '" + task.idValue()
                            + "' expects input type " + expectedInput.getName()
                            + " but received output type " + providedOutput.getName()
                            + " from task '" + input.ref().idValue() + "'"
            );
        }

        then(task.idValue());
        return new TypedTaskNode<>(task.toRef());
    }

    // -------------------------------------------------------------------------
    // Fork / Join
    // -------------------------------------------------------------------------

    /**
     * Starts parallel branches from the current tail(s).
     *
     * @param branches one or more branch consumers; must not be null or empty
     * @return this builder
     */
    FlowBuilder fork(java.util.function.Consumer<FlowBranch>... branches);

    /**
     * Merges all current parallel tails into a single task.
     * Semantically equivalent to {@link #then(String)}.
     *
     * @param taskId the id of the join task; must not be null or blank
     * @return this builder
     */
    FlowBuilder join(String taskId);

    /**
     * Merges all current parallel tails into the given typed task reference.
     *
     * @param ref the typed reference to the join task; must not be null
     * @param <T> the output type of the join task
     * @return this builder
     */
    default <T> FlowBuilder join(TaskRef<T> ref) {
        java.util.Objects.requireNonNull(ref, "ref");
        return join(ref.idValue());
    }

    /**
     * Merges all current parallel tails using a typed task definition.
     * Returns a typed node for the join task's output.
     *
     * @param task the task definition for the join task
     * @param <I>  the input type
     * @param <O>  the output type
     * @return a {@link TypedTaskNode} representing the join task's output
     */
    default <I, O> TypedTaskNode<O> join(TaskDefinition<I, O> task) {
        java.util.Objects.requireNonNull(task, "task");
        join(task.idValue());
        return new TypedTaskNode<>(task.toRef());
    }

    /**
     * Finalizes the workflow definition and produces an executable plan.
     *
     * @return the constructed {@link WorkflowExecutionPlan}
     */
    WorkflowExecutionPlan build();
}