package org.royada.flowforge.spring.dsl.internal;

import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
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
    public Class<I> inputType() {
        return delegate.inputType();
    }

    @Override
    public Class<O> outputType() {
        return delegate.outputType();
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