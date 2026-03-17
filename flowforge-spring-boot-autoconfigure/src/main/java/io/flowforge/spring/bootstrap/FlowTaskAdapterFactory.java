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
        // Ajusta exactamente a lo que adapt(...) soporta HOY.
        // Lo más probable por tus tests: FlowTaskHandler (Spring API o core API).
        return FlowTaskHandler.class.isAssignableFrom(beanType)
                // Si todavía existe el alias spring.api:
                || FlowTaskHandler.class.isAssignableFrom(beanType)
                // Si también soportas Task directo:
                || Task.class.isAssignableFrom(beanType);
    }
    @SuppressWarnings("unchecked")
    public Task<?, ?> adapt(Object bean, FlowTask ann) {

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