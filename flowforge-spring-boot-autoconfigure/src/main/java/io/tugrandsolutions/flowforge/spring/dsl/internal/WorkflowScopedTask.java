package io.tugrandsolutions.flowforge.spring.dsl.internal;

import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

final class WorkflowScopedTask<I, O> implements Task<I, O> {

    private final Task<I, O> delegate;
    private final Set<TaskId> deps;

    WorkflowScopedTask(Task<I, O> delegate, Set<TaskId> deps) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.deps = Set.copyOf(Objects.requireNonNull(deps, "deps"));
    }

    @Override
    public TaskId id() {
        return delegate.id();
    }

    @Override
    public Set<TaskId> dependencies() {
        // workflow defines deps; we DO NOT force the task to know the workflow.
        // If you want backward compatibility with task-native deps, use union:
        // return union(delegate.dependencies(), deps);
        return deps;
    }

    @Override
    public boolean optional() {
        return delegate.optional();
    }

    @Override
    public Mono<O> execute(I input, ReactiveExecutionContext ctx) {
        return delegate.execute(input, ctx);
    }
}