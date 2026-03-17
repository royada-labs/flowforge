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
    private final Class<I> inputType;
    private final Class<O> outputType;

    private final FlowTaskHandler<I, O> handler;

    public FlowTaskAdapter(
            TaskId id,
            Set<TaskId> dependencies,
            boolean optional,
            Class<I> inputType,
            Class<O> outputType,
            FlowTaskHandler<I, O> handler
    ) {
        this.id = Objects.requireNonNull(id);
        this.dependencies = Set.copyOf(dependencies);
        this.optional = optional;
        this.inputType = inputType;
        this.outputType = outputType;
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public Class<I> inputType() {
        return inputType;
    }

    @Override
    public Class<O> outputType() {
        return outputType;
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
