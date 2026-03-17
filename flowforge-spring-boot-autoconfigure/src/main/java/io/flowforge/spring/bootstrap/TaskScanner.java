package io.flowforge.spring.bootstrap;

import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.TaskHandler;
import io.flowforge.spring.registry.TaskDefinitionRegistry;
import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.spring.registry.TaskProvider;
import io.flowforge.task.TaskDefinition;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class TaskScanner implements BeanFactoryPostProcessor {

    private final TaskHandlerRegistry registry;
    private final TaskDefinitionRegistry definitionRegistry;
    private final FlowTaskAdapterFactory adapterFactory;
    private final AnnotatedMethodTaskAdapterFactory methodAdapterFactory;

    public TaskScanner(TaskHandlerRegistry registry, TaskDefinitionRegistry definitionRegistry) {
        this(registry, definitionRegistry, new FlowTaskAdapterFactory(), new AnnotatedMethodTaskAdapterFactory());
    }

    TaskScanner(
            TaskHandlerRegistry registry,
            TaskDefinitionRegistry definitionRegistry,
            FlowTaskAdapterFactory adapterFactory,
            AnnotatedMethodTaskAdapterFactory methodAdapterFactory
    ) {
        this.registry = registry;
        this.definitionRegistry = definitionRegistry;
        this.adapterFactory = adapterFactory;
        this.methodAdapterFactory = methodAdapterFactory;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {

        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            Class<?> type = beanFactory.getType(beanName);
            FlowTask beanFlowTask = beanFactory.findAnnotationOnBean(beanName, FlowTask.class);
            if (beanFlowTask != null) {
                registerBeanLevelTask(beanFactory, bd, beanName, type, beanFlowTask);
            }

            TaskHandler taskHandler = beanFactory.findAnnotationOnBean(beanName, TaskHandler.class);
            if (taskHandler != null) {
                registerTaskHandlerMethods(beanFactory, beanName, type);
            }
        }
    }

    private void registerBeanLevelTask(
            ConfigurableListableBeanFactory beanFactory,
            BeanDefinition bd,
            String beanName,
            Class<?> type,
            FlowTask annotation
    ) {
        if (bd.isSingleton()) {
            bd.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        }

        boolean supported =
                beanFactory.isTypeMatch(beanName, FlowTaskHandler.class)
                        || beanFactory.isTypeMatch(beanName, Task.class);

        if (!supported) {
            throw new IllegalStateException(
                    "@FlowTask bean does not implement FlowTaskHandler.  " +
                            "beanName=" + beanName + ", type=" + (type == null ? "unknown" : type.getName())
            );
        }

        TaskId taskId = TaskId.of(annotation.id());
        Class<?>[] inferredTypes = inferTypes(beanFactory, beanName, annotation, bd);
        TaskDefinition<?, ?> definition = definition(taskId, inferredTypes[0], inferredTypes[1]);
        definitionRegistry.register(definition, beanName);

        String implClass = type != null ? type.getName().replace('.', '/') : "";
        definitionRegistry.registerMethodRef(implClass, beanName, definition);

        Supplier<Object> beanSupplier = () -> beanFactory.getBean(beanName);
        registry.register(new TaskProvider() {
            @Override public TaskId id() { return taskId; }

            @Override
            public Task<?, ?> get() {
                Object bean = beanSupplier.get();
                return adapterFactory.adapt(bean, annotation);
            }
        });
    }

    private void registerTaskHandlerMethods(
            ConfigurableListableBeanFactory beanFactory,
            String beanName,
            Class<?> type
    ) {
        if (type == null) {
            return;
        }

        String implClass = type.getName().replace('.', '/');
        Supplier<Object> beanSupplier = () -> beanFactory.getBean(beanName);

        for (Method method : type.getDeclaredMethods()) {
            FlowTask ann = method.getAnnotation(FlowTask.class);
            if (ann == null) {
                continue;
            }

            Class<?>[] inferred = inferMethodTypes(ann, method);
            TaskId taskId = TaskId.of(ann.id());
            TaskDefinition<?, ?> definition = definition(taskId, inferred[0], inferred[1]);
            definitionRegistry.register(definition, method.getName());
            definitionRegistry.registerMethodRef(implClass, method.getName(), definition);

            registry.register(new TaskProvider() {
                @Override
                public TaskId id() {
                    return taskId;
                }

                @Override
                public Task<?, ?> get() {
                    Object bean = beanSupplier.get();
                    return methodAdapterFactory.adapt(bean, method, ann, inferred[0], inferred[1]);
                }
            });
        }
    }

    private static Class<?>[] inferTypes(
            ConfigurableListableBeanFactory beanFactory,
            String beanName,
            FlowTask annotation,
            BeanDefinition beanDefinition
    ) {
        Class<?> input = annotation.inputType();
        Class<?> output = annotation.outputType();
        if (input != Object.class || output != Object.class) {
            return new Class<?>[]{input, output};
        }

        String factoryBeanName = beanDefinition.getFactoryBeanName();
        String factoryMethodName = beanDefinition.getFactoryMethodName();
        if (factoryBeanName == null || factoryMethodName == null) {
            return new Class<?>[]{Object.class, Object.class};
        }

        Class<?> factoryType = beanFactory.getType(factoryBeanName);
        if (factoryType == null) {
            return new Class<?>[]{Object.class, Object.class};
        }

        for (Method method : factoryType.getDeclaredMethods()) {
            if (!method.getName().equals(factoryMethodName) || method.getParameterCount() != 0) {
                continue;
            }
            ResolvableType type = ResolvableType.forMethodReturnType(method).as(FlowTaskHandler.class);
            Class<?> inferredInput = type.getGeneric(0).resolve(Object.class);
            Class<?> inferredOutput = type.getGeneric(1).resolve(Object.class);
            return new Class<?>[]{inferredInput, inferredOutput};
        }

        return new Class<?>[]{Object.class, Object.class};
    }

    private static Class<?>[] inferMethodTypes(FlowTask annotation, Method method) {
        Class<?> input = annotation.inputType();
        Class<?> output = annotation.outputType();
        if (input == Object.class) {
            input = inferInputType(method);
        }
        if (output == Object.class) {
            output = inferOutputType(method);
        }
        return new Class<?>[]{input, output};
    }

    private static Class<?> inferInputType(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            return Void.class;
        }
        if (params.length == 1) {
            return ReactiveExecutionContext.class.isAssignableFrom(params[0]) ? Void.class : params[0];
        }
        if (params.length == 2) {
            return params[0];
        }
        throw new IllegalStateException("@FlowTask method supports up to 2 parameters: " + method);
    }

    private static Class<?> inferOutputType(Method method) {
        if (Mono.class.isAssignableFrom(method.getReturnType())) {
            return ResolvableType.forMethodReturnType(method)
                    .as(Mono.class)
                    .getGeneric(0)
                    .resolve(Object.class);
        }
        return method.getReturnType();
    }

    @SuppressWarnings("unchecked")
    private static <I, O> TaskDefinition<I, O> definition(TaskId id, Class<?> input, Class<?> output) {
        return TaskDefinition.of(id, (Class<I>) input, (Class<O>) output);
    }
}
