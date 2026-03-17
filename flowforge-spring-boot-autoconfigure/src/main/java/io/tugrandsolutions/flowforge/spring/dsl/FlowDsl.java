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
     * returning both a builder and a typed node for the starting task's output.
     *
     * <p>Example:
     * <pre>{@code
     * TypedFlowStart<UserProfile> start = dsl.startTyped(fetchUser());
     * TypedTaskNode<EnrichedUser> enriched = start.builder().then(enrichUser(), start.node());
     * WorkflowExecutionPlan plan = start.builder().build();
     * }</pre>
     *
     * @param task the task definition for the starting task
     * @param <I>  the input type of the starting task
     * @param <O>  the output type of the starting task
     * @return a {@link TypedFlowStart} containing the builder and the typed node
     */
    default <I, O> TypedFlowStart<O> startTyped(TaskDefinition<I, O> task) {
        java.util.Objects.requireNonNull(task, "task");
        FlowBuilder builder = start(task.idValue());
        TypedTaskNode<O> node = new TypedTaskNode<>(task.toRef());
        return new TypedFlowStart<>(builder, node);
    }

    /**
     * The result of starting a workflow with a typed {@link TaskDefinition}.
     * Bundles the {@link FlowBuilder} for continued chaining with the
     * {@link TypedTaskNode} representing the starting task's output.
     *
     * @param <O> the output type of the starting task
     */
    record TypedFlowStart<O>(FlowBuilder builder, TypedTaskNode<O> node) {
        /**
         * Creates a typed flow start.
         *
         * @param builder the flow builder; must not be null
         * @param node    the typed node; must not be null
         */
        public TypedFlowStart {
            java.util.Objects.requireNonNull(builder, "builder");
            java.util.Objects.requireNonNull(node, "node");
        }
    }
}