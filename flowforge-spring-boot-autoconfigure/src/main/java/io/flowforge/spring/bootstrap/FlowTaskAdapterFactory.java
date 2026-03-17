package io.flowforge.spring.bootstrap;

import io.flowforge.spring.adapter.FlowTaskAdapter;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class FlowTaskAdapterFactory {

    public boolean supports(Class<?> beanType) {
        return FlowTaskHandler.class.isAssignableFrom(beanType)
                || Task.class.isAssignableFrom(beanType);
    }

    public Task<?, ?> adapt(Object bean, FlowTask ann) {

        if (!(bean instanceof FlowTaskHandler<?, ?> rawHandler)) {
            throw new IllegalStateException(
                    "@FlowTask requires bean implements FlowTaskHandler: "
                            + bean.getClass().getName()
            );
        }

        TaskId id = TaskId.of(ann.id());

        Set<TaskId> deps = Arrays.stream(ann.dependsOn())
                .map(TaskId::of)
                .collect(Collectors.toUnmodifiableSet());

        return adaptTyped(rawHandler, ann, id, deps);
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Task<I, O> adaptTyped(
            FlowTaskHandler<?, ?> rawHandler,
            FlowTask ann,
            TaskId id,
            Set<TaskId> deps
    ) {
        return new FlowTaskAdapter<>(
                id,
                deps,
                ann.optional(),
                (Class<I>) ann.inputType(),
                (Class<O>) ann.outputType(),
                (FlowTaskHandler<I, O>) rawHandler
        );
    }
}
