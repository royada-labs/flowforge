package io.flowforge.spring.bootstrap;

import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.spring.registry.TaskProvider;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.function.Supplier;

public final class TaskScanner implements BeanFactoryPostProcessor {

    private final TaskHandlerRegistry registry;
    private final FlowTaskAdapterFactory adapterFactory;

    public TaskScanner(TaskHandlerRegistry registry) {
        this(registry, new FlowTaskAdapterFactory());
    }

    TaskScanner(TaskHandlerRegistry registry, FlowTaskAdapterFactory adapterFactory) {
        this.registry = registry;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {

        for (String beanName : beanFactory.getBeanDefinitionNames()) {

            FlowTask annotation = beanFactory.findAnnotationOnBean(beanName, FlowTask.class);
            if (annotation == null) continue;

            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            if (bd.isSingleton()) {
                bd.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
            }

            // 1) Validación temprana SIN instanciar
            Class<?> type = beanFactory.getType(beanName);
            boolean supported =
                    beanFactory.isTypeMatch(beanName, FlowTaskHandler.class)
                            || beanFactory.isTypeMatch(beanName, Task.class);
            // agrega aquí otros tipos soportados por tu adapter

            if (!supported) {
                throw new IllegalStateException(
                        "@FlowTask bean does not implement FlowTaskHandler.  " +
                                "beanName=" + beanName + ", type=" + (type == null ? "unknown" : type.getName())
                );
            }

            // 2) Registro lazy: provider crea instancia en runtime y adapta
            TaskId taskId = TaskId.of(annotation.id());
            Supplier<Object> beanSupplier = () -> beanFactory.getBean(beanName);

            registry.register(new TaskProvider<>() {
                @Override public TaskId id() { return taskId; }

                @Override
                public Task<Object, Object> get() {
                    Object bean = beanSupplier.get();
                    return (Task<Object, Object>) adapterFactory.adapt(bean, annotation);
                }
            });
        }
    }
}