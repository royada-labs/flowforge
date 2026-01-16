package io.tugrandsolutions.flowforge.task;

import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

public abstract class BasicTask<I, O>  implements Task<I, O> {

    private final TaskId id;

    protected BasicTask(TaskId id) {
        this.id = id;
    }

    @Override
    public final TaskId id() {
        return id;
    }

    @Override
    public final Mono<O> execute(
            I input,
            ReactiveExecutionContext context
    ) {
        return Mono.defer(() -> doExecute(input, context))
                .doOnNext(result -> context.put(id, result));
    }

    protected abstract Mono<O> doExecute(
            I input,
            ReactiveExecutionContext context
    );
}