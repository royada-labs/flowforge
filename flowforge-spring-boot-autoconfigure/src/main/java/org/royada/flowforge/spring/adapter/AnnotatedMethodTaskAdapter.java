package org.royada.flowforge.spring.adapter;

import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime task adapter backed by a pre-resolved method handle from a {@code @TaskHandler} bean method.
 *
 * @param <I> input type
 * @param <O> output type
 */
public final class AnnotatedMethodTaskAdapter<I, O> implements Task<I, O> {

    /**
     * Supported invocation signatures for adapted methods.
     */
    public enum InvocationKind {
        /** Method accepts no arguments. */
        NO_ARGS,
        /** Method accepts only task input. */
        INPUT_ONLY,
        /** Method accepts only execution context. */
        CONTEXT_ONLY,
        /** Method accepts task input and execution context. */
        INPUT_AND_CONTEXT
    }

    private final TaskId id;
    private final Set<TaskId> dependencies;
    private final boolean optional;
    private final Class<I> inputType;
    private final Class<O> outputType;
    private final MethodHandle handle;
    private final InvocationKind kind;
    private final boolean monoReturn;

    /**
     * Creates a method-backed task adapter.
     *
     * @param id task id
     * @param dependencies task dependencies
     * @param optional whether task is optional
     * @param inputType input type
     * @param outputType output type
     * @param handle bound method handle
     * @param kind invocation signature kind
     * @param monoReturn whether method returns {@link Mono}
     */
    public AnnotatedMethodTaskAdapter(
            TaskId id,
            Set<TaskId> dependencies,
            boolean optional,
            Class<I> inputType,
            Class<O> outputType,
            MethodHandle handle,
            InvocationKind kind,
            boolean monoReturn
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.dependencies = Set.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
        this.optional = optional;
        this.inputType = Objects.requireNonNull(inputType, "inputType");
        this.outputType = Objects.requireNonNull(outputType, "outputType");
        this.handle = Objects.requireNonNull(handle, "handle");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.monoReturn = monoReturn;
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
    public Class<I> inputType() {
        return inputType;
    }

    @Override
    public Class<O> outputType() {
        return outputType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<O> execute(I input, ReactiveExecutionContext ctx) {
        return Mono.defer(() -> {
            try {
                Object out = switch (kind) {
                    case NO_ARGS -> handle.invokeWithArguments();
                    case INPUT_ONLY -> handle.invokeWithArguments(input);
                    case CONTEXT_ONLY -> handle.invokeWithArguments(ctx);
                    case INPUT_AND_CONTEXT -> handle.invokeWithArguments(input, ctx);
                };

                if (monoReturn) {
                    return (Mono<O>) out;
                }
                return Mono.justOrEmpty((O) out);
            } catch (Throwable e) {
                return Mono.error(e);
            }
        });
    }
}

