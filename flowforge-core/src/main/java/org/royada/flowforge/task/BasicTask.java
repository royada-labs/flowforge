package org.royada.flowforge.task;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

/**
 * Base helper implementation for {@link Task}.
 *
 * @param <I> input type
 * @param <O> output type
 */
public abstract class BasicTask<I, O>  implements Task<I, O> {

    private final TaskId id;
    private final Class<I> inputType;
    private final Class<O> outputType;

    /**
     * Creates a typed task base.
     *
     * @param id task id
     * @param inputType input type
     * @param outputType output type
     */
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

        /**
         * Executes task logic.
         *
         * @param input task input
         * @param context execution context
         * @return output publisher
         */
    protected abstract Mono<O> doExecute(
            I input,
            ReactiveExecutionContext context
    );
}