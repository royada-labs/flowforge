package org.royada.flowforge.spring.registry;

import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.spring.bootstrap.FlowTaskAdapterFactory;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;

import java.util.function.Supplier;

/**
 * {@link TaskProvider} implementation that resolves Spring beans lazily and adapts them to tasks.
 */
public final class SpringTaskProvider implements TaskProvider {

    private final TaskId id;
    private final Supplier<Object> beanSupplier;
    private final FlowTaskAdapterFactory adapterFactory;
    private final FlowTask annotation;

    /**
     * Creates a provider backed by a Spring bean supplier.
     *
     * @param id task id
     * @param beanSupplier supplier resolving bean instances
     * @param adapterFactory adapter factory used to build {@link Task} wrappers
     * @param annotation task annotation metadata
     */
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
    public Task<?, ?> get() {
        Object bean = beanSupplier.get();                 // instancia por ejecución (prototype)
        return adapterFactory.adapt(bean, annotation);
    }
}
