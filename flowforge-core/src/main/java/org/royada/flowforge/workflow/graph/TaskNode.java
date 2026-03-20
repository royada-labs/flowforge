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

public final class TaskNode {

    private final TaskDescriptor descriptor;
    private final Set<TaskNode> dependencies = new HashSet<>();
    private final Set<TaskNode> dependents = new HashSet<>();

    public TaskNode(TaskDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    public TaskId id() {
        return descriptor.id();
    }

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

    public Set<TaskNode> dependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public Set<TaskNode> dependents() {
        return Collections.unmodifiableSet(dependents);
    }

    public boolean isRoot() {
        return dependencies.isEmpty();
    }

    public boolean isLeaf() {
        return dependents.isEmpty();
    }

    public Mono<Object> execute(Object input, ReactiveExecutionContext context) {
        Mono<Object> raw = executeTyped(descriptor.task(), input, context);
        return descriptor.policy().apply(raw);
    }

    private static <I, O> Mono<Object> executeTyped(Task<I, O> task, Object input, ReactiveExecutionContext context) {
        I typedInput = input == null ? null : task.inputType().cast(input);
        return task.execute(typedInput, context).map(result -> (Object) result);
    }

    public Mono<TaskResult> executeWithResult(
            Object input,
            ReactiveExecutionContext context
    ) {
        return execute(input, context) // <- aquí debe estar la policy aplicada
                .map(result -> (TaskResult) new TaskResult.Success(result))
                .onErrorResume(error ->
                        Mono.just(
                                descriptor.optional()
                                        ? new TaskResult.Skipped()
                                        : new TaskResult.Failure(error)
                        )
                );
    }
}
