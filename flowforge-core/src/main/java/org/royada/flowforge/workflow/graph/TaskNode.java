package org.royada.flowforge.workflow.graph;

import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskDescriptor;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.task.TaskResult;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a node in the workflow execution graph.
 */
public final class TaskNode {

    private final TaskDescriptor descriptor;
    private final Set<TaskNode> dependencies = new HashSet<>();
    private final Set<TaskNode> dependents = new HashSet<>();

    /**
     * Creates a new node with the given task descriptor.
     * 
     * @param descriptor the task descriptor
     */
    public TaskNode(TaskDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    /**
     * Returns the ID of the task in this node.
     * 
     * @return the task ID
     */
    public TaskId id() {
        return descriptor.id();
    }

    /**
     * Returns the task descriptor.
     * 
     * @return the descriptor
     */
    public TaskDescriptor descriptor() {
        return descriptor;
    }

    /* package-private */
    void addDependency(TaskNode dependency) {
        dependencies.add(dependency);
    }

    /* package-private */
    void addDependent(TaskNode dependent) {
        dependents.add(dependent);
    }

    /**
     * Returns the set of dependency nodes.
     * 
     * @return an unmodifiable set of dependency nodes
     */
    public Set<TaskNode> dependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Returns the set of dependent nodes.
     * 
     * @return an unmodifiable set of dependent nodes
     */
    public Set<TaskNode> dependents() {
        return Collections.unmodifiableSet(dependents);
    }

    /**
     * Returns whether this node is a root (has no dependencies).
     * 
     * @return {@code true} if root, {@code false} otherwise
     */
    public boolean isRoot() {
        return dependencies.isEmpty();
    }

    /**
     * Returns whether this node is a leaf (has no dependents).
     * 
     * @return {@code true} if leaf, {@code false} otherwise
     */
    public boolean isLeaf() {
        return dependents.isEmpty();
    }

    /**
     * Executes the task node with the given input and context.
     * 
     * @param input the input object
     * @param context the execution context
     * @return a Mono that completes with the task result
     */
    public Mono<Object> execute(Object input, ReactiveExecutionContext context) {
        return descriptor.policy().apply(
                Mono.defer(() -> executeTyped(descriptor.task(), input, context))
        );
    }

    private static <I, O> Mono<Object> executeTyped(Task<I, O> task, Object input, ReactiveExecutionContext context) {
        I typedInput = input == null ? null : task.inputType().cast(input);
        return task.execute(typedInput, context).map(result -> (Object) result);
    }

    /**
     * Executes the task node and wraps the result in a {@link TaskResult}.
     * 
     * @param input the input object
     * @param context the execution context
     * @return a Mono that completes with the {@link TaskResult}
     */
    public Mono<TaskResult> executeWithResult(
            Object input,
            ReactiveExecutionContext context
    ) {
        return execute(input, context)
                .map(result -> (TaskResult) new TaskResult.Success(result))
                .defaultIfEmpty(new TaskResult.Success(null))
                .onErrorResume(error ->
                        Mono.just(
                                descriptor.optional()
                                        ? new TaskResult.Skipped()
                                        : new TaskResult.Failure(error)
                        )
                );
    }
}
