package org.royada.flowforge.task;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

public abstract class BasicTask<I, O>  implements Task<I, O> {

    private final TaskId id;
    private final Class<I> inputType;
    private final Class<O> outputType;

    protected BasicTask(TaskId id, Class<I> inputType, Class<O> outputType) {
        this.id = id;
        this.inputType = inputType;
        this.outputType = outputType;
    }

    @Override
    public final TaskId id() {
        return id;
    }

    @Override
    public final Class<I> inputType() {
        return inputType;
    }

    @Override
    public final Class<O> outputType() {
        return outputType;
    }


    @Override
    public final Mono<O> execute(
            I input,
            ReactiveExecutionContext context
    ) {
        return Mono.defer(() -> doExecute(input, context))
                .doOnNext(result -> context.put(outputKey(), result));
    }

    protected abstract Mono<O> doExecute(
            I input,
            ReactiveExecutionContext context
    );
}