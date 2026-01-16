package io.tugrandsolutions.flowforge.workflow.graph;

import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskDescriptor;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.task.TaskResult;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
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

    @SuppressWarnings("unchecked")
    public Mono<Object> execute(Object input, ReactiveExecutionContext context) {
        Task<Object, Object> task = (Task<Object, Object>) descriptor.task();

        Mono<Object> raw = task.execute(input, context).cast(Object.class);

        return descriptor.policy().apply(raw);
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