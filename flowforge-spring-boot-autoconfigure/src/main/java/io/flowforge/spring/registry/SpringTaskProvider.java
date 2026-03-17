package io.flowforge.spring.registry;

import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.bootstrap.FlowTaskAdapterFactory;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import org.springframework.beans.factory.ObjectProvider;

import java.util.function.Supplier;

public final class SpringTaskProvider implements TaskProvider<Object, Object> {

    private final TaskId id;
    private final Supplier<Object> beanSupplier;
    private final FlowTaskAdapterFactory adapterFactory;
    private final FlowTask annotation;

    public SpringTaskProvider(
            TaskId id,
            Supplier<Object> beanSupplier,
            FlowTaskAdapterFactory adapterFactory,
            FlowTask annotation
    ) {
        this.id = id;
        this.beanSupplier = beanSupplier;
        this.adapterFactory = adapterFactory;
        this.annotation = annotation;
    }

    @Override public TaskId id() { return id; }

    @Override
    public Task<Object, Object> get() {
        Object bean = beanSupplier.get();                 // instancia por ejecución (prototype)
        return (Task<Object, Object>) adapterFactory.adapt(bean, annotation);
    }
}