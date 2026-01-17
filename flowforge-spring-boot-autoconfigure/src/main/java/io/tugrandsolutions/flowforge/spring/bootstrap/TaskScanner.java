package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.task.Task;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

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

            // CLAVE: esto detecta @FlowTask tanto en clase como en método @Bean
            FlowTask annotation = beanFactory.findAnnotationOnBean(beanName, FlowTask.class);
            if (annotation == null) {
                continue;
            }

            Object bean = beanFactory.getBean(beanName); // instancia SOLO los anotados
            Task<?, ?> task = adapterFactory.adapt(bean, annotation);
            registry.register(task);
        }
    }
}
