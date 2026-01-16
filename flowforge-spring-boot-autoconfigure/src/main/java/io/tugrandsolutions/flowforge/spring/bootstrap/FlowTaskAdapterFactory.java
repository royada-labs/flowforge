package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.spring.adapter.FlowTaskAdapter;
import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.api.FlowTaskHandler;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

final class FlowTaskAdapterFactory {

    @SuppressWarnings("unchecked")
    Task<?, ?> adapt(Object bean, FlowTask ann) {

        if (!(bean instanceof FlowTaskHandler<?, ?> handler)) {
            throw new IllegalStateException(
                    "@FlowTask requires bean implements FlowTaskHandler: "
                            + bean.getClass().getName()
            );
        }

        TaskId id = new TaskId(ann.id());

        Set<TaskId> deps = Arrays.stream(ann.dependsOn())
                .map(TaskId::new)
                .collect(Collectors.toUnmodifiableSet());

        return new FlowTaskAdapter<>(
                id,
                deps,
                ann.optional(),
                (FlowTaskHandler<Object, Object>) handler
        );
    }
}