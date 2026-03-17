package io.flowforge.spring.adapter;

import io.flowforge.api.FlowTaskHandler;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;

import java.util.Objects;
import java.util.Set;
import io.flowforge.task.Task;
import reactor.core.publisher.Mono;

public final class FlowTaskAdapter<I, O> implements Task<I, O> {

    private final TaskId id;
    private final Set<TaskId> dependencies;
    private final boolean optional;
    private final FlowTaskHandler<I, O> handler;

    public FlowTaskAdapter(
            TaskId id,
            Set<TaskId> dependencies,
            boolean optional,
            FlowTaskHandler<I, O> handler
    ) {
        this.id = Objects.requireNonNull(id);
        this.dependencies = Set.copyOf(dependencies);
        this.optional = optional;
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public TaskId id() {
        return id;
    }

    @Override
    public Set<TaskId> dependencies() {
        return dependencies;
    }

    @Override
    public boolean optional() {
        return optional;
    }

    @Override
    public Mono<O> execute(I input, ReactiveExecutionContext ctx) {
        Mono<O> mono = handler.execute(input, ctx);
        if (mono == null) {
            return Mono.error(
                    new IllegalStateException(
                            "FlowTaskHandler returned null Mono for task " + id
                    )
            );
        }
        return mono;
    }
}
