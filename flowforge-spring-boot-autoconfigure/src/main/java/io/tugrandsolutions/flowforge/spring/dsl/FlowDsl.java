package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.dsl.TypedTaskNode;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.task.TaskRef;

/**
 * Entry point for defining FlowForge workflows using a fluent DSL.
 *
 * <p>Both string-based, {@link TaskRef}-based, and fully-typed
 * {@link TaskDefinition}-based overloads are supported.
 */
public interface FlowDsl {

    /**
     * Starts a workflow definition with the given task id string.
     *
     * @param taskId the id of the first task; must not be null or blank
     * @return a {@link FlowBuilder} to continue the workflow definition
     */
    FlowBuilder start(String taskId);

    /**
     * Starts a workflow definition using a typed {@link TaskRef}.
     *
     * @param ref the typed reference to the first task; must not be null
     * @param <T> the output type of the starting task
     * @return a {@link FlowBuilder} to continue the workflow definition
     */
    default <T> FlowBuilder start(TaskRef<T> ref) {
        java.util.Objects.requireNonNull(ref, "ref");
        return start(ref.idValue());
    }

    /**
     * Starts a workflow definition using a fully-typed {@link TaskDefinition},
     * returning a builder tracking the output type for seamless chaining.
     *
     * <p>Example:
     * <pre>{@code
     * WorkflowExecutionPlan plan = dsl.startTyped(fetchUser())
     *     .then(enrichUser())
     *     .build();
     * }</pre>
     *
     * @param task the task definition for the starting task
     * @param <I>  the input type of the starting task
     * @param <O>  the output type of the starting task
     * @return a {@link TypedFlowBuilder} tracking the output type {@code O}
     */
    default <I, O> TypedFlowBuilder<O> startTyped(TaskDefinition<I, O> task) {
        java.util.Objects.requireNonNull(task, "task");
        FlowBuilder builder = start(task.idValue());
        TypedTaskNode<O> node = new TypedTaskNode<>(task.toRef());
        return new TypedFlowBuilder<>(builder, node);
    }

    /**
     * @deprecated Use {@link #startTyped(TaskDefinition)} which returns {@link TypedFlowBuilder}.
     */
    @Deprecated(since = "0.4.0", forRemoval = true)
    record TypedFlowStart<O>(FlowBuilder builder, TypedTaskNode<O> node) {
        public TypedFlowStart {
            java.util.Objects.requireNonNull(builder, "builder");
            java.util.Objects.requireNonNull(node, "node");
        }
    }
}