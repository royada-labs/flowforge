package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import io.tugrandsolutions.flowforge.task.Task;
import org.springframework.context.ApplicationContextAware;

public final class TaskScanner
        implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final TaskHandlerRegistry registry;
    private final FlowTaskAdapterFactory adapterFactory =
            new FlowTaskAdapterFactory();

    public TaskScanner(TaskHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(
            @NonNull Object bean,
            @NonNull String beanName
    ) throws BeansException {

        FlowTask annotation =
                applicationContext.findAnnotationOnBean(beanName, FlowTask.class);

        if (annotation == null) {
            return bean;
        }

        Task<?, ?> task = adapterFactory.adapt(bean, annotation);

        registry.register(task);

        return bean;
    }
}