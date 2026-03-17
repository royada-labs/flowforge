package io.flowforge.spring.adapter;

import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime task adapter backed by a pre-resolved method handle from a {@code @TaskHandler} bean method.
 */
public final class AnnotatedMethodTaskAdapter<I, O> implements Task<I, O> {

    public enum InvocationKind {
        NO_ARGS,
        INPUT_ONLY,
        CONTEXT_ONLY,
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
    }
}

