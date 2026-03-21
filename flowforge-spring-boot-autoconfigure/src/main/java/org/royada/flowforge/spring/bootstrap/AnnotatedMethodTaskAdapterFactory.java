package org.royada.flowforge.spring.bootstrap;

import org.royada.flowforge.spring.adapter.AnnotatedMethodTaskAdapter;
import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory that adapts {@code @FlowTask} bean methods into runtime task instances.
 */
public final class AnnotatedMethodTaskAdapterFactory {

    /**
     * Creates a new factory.
     */
    public AnnotatedMethodTaskAdapterFactory() {
    }

    /**
     * Adapts an annotated bean method into a {@link Task}.
     *
     * @param bean bean instance
     * @param method annotated method
     * @param ann {@code @FlowTask} annotation
     * @param inputType inferred input type
     * @param outputType inferred output type
     * @return adapted task
     */
    @SuppressWarnings("unchecked")
    public Task<?, ?> adapt(
            Object bean,
            Method method,
            FlowTask ann,
            Class<?> inputType,
            Class<?> outputType
    ) {
        try {
            method.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(method).bindTo(bean);
            boolean monoReturn = Mono.class.isAssignableFrom(method.getReturnType());

            return new AnnotatedMethodTaskAdapter<>(
                    TaskId.of(ann.id()),
                    dependencies(ann),
                    ann.optional(),
                    (Class<Object>) inputType,
                    (Class<Object>) outputType,
                    handle,
                    invocationKind(method),
                    monoReturn
            );
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to bind @FlowTask method: " + method, e);
        }
    }

    private static Set<TaskId> dependencies(FlowTask ann) {
        return Arrays.stream(ann.dependsOn())
                .map(TaskId::of)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static AnnotatedMethodTaskAdapter.InvocationKind invocationKind(Method method) {
        Class<?>[] p = method.getParameterTypes();
        return switch (p.length) {
            case 0 -> AnnotatedMethodTaskAdapter.InvocationKind.NO_ARGS;
            case 1 -> ReactiveExecutionContext.class.isAssignableFrom(p[0])
                    ? AnnotatedMethodTaskAdapter.InvocationKind.CONTEXT_ONLY
                    : AnnotatedMethodTaskAdapter.InvocationKind.INPUT_ONLY;
            case 2 -> AnnotatedMethodTaskAdapter.InvocationKind.INPUT_AND_CONTEXT;
            default -> throw new IllegalStateException(
                    "@FlowTask method supports up to 2 parameters: " + method);
        };
    }
}
