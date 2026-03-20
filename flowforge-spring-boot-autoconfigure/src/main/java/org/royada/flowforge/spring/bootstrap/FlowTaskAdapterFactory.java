package org.royada.flowforge.spring.bootstrap;

import org.royada.flowforge.exception.TaskRegistrationException;
import org.royada.flowforge.exception.WorkflowConfigurationException;
import org.royada.flowforge.spring.adapter.FlowTaskAdapter;
import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.api.FlowTaskHandler;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapts Spring beans annotated with {@link FlowTask} into executable {@link Task} instances.
 */
public final class FlowTaskAdapterFactory {

    /**
     * Creates a factory instance.
     */
    public FlowTaskAdapterFactory() {
    }

    /**
     * Checks whether the bean type is supported for adaptation.
     *
     * @param beanType bean class
     * @return {@code true} if supported
     */
    public boolean supports(Class<?> beanType) {
        return FlowTaskHandler.class.isAssignableFrom(beanType)
                || Task.class.isAssignableFrom(beanType);
    }

    /**
     * Adapts a bean using annotation-declared types (or defaults when unspecified).
     *
     * @param bean source bean
     * @param ann task annotation
     * @return adapted task
     */
    public Task<?, ?> adapt(Object bean, FlowTask ann) {
        return adapt(bean, ann, ann.inputType(), ann.outputType());
    }

    /**
     * Adapts a bean using inferred generic types and validates compatibility with annotation values.
     *
     * @param bean source bean
     * @param ann task annotation
     * @param inferredInputType inferred input type
     * @param inferredOutputType inferred output type
     * @return adapted task
     */
    public Task<?, ?> adapt(Object bean, FlowTask ann, Class<?> inferredInputType, Class<?> inferredOutputType) {
        if (bean instanceof Task<?, ?> task) {
            validateTaskBeanTypeCompatibility(task, ann, inferredInputType, inferredOutputType);
            return task;
        }

        if (!(bean instanceof FlowTaskHandler<?, ?> rawHandler)) {
            throw new TaskRegistrationException(
                    "@FlowTask requires bean implements FlowTaskHandler: "
                            + bean.getClass().getName()
            );
        }

        TaskId id = TaskId.of(ann.id());

        Set<TaskId> deps = Arrays.stream(ann.dependsOn())
                .map(TaskId::of)
                .collect(Collectors.toUnmodifiableSet());

        Class<?> inputType = resolveEffectiveType("input", ann.inputType(), inferredInputType, ann.id());
        Class<?> outputType = resolveEffectiveType("output", ann.outputType(), inferredOutputType, ann.id());
        return adaptTyped(rawHandler, ann, id, deps, inputType, outputType);
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Task<I, O> adaptTyped(
            FlowTaskHandler<?, ?> rawHandler,
            FlowTask ann,
            TaskId id,
            Set<TaskId> deps,
            Class<?> inputType,
            Class<?> outputType
    ) {
        return new FlowTaskAdapter<>(
                id,
                deps,
                ann.optional(),
                (Class<I>) inputType,
                (Class<O>) outputType,
                (FlowTaskHandler<I, O>) rawHandler
        );
    }

    private static Class<?> resolveEffectiveType(String label, Class<?> explicit, Class<?> inferred, String taskId) {
        if (explicit != Object.class && inferred != Object.class && !explicit.equals(inferred)) {
            throw new WorkflowConfigurationException(
                    "@FlowTask " + label + " type mismatch for task '" + taskId + "': annotation="
                            + explicit.getName() + ", inferred=" + inferred.getName()
            );
        }
        return explicit != Object.class ? explicit : inferred;
    }

    private static void validateTaskBeanTypeCompatibility(
            Task<?, ?> task,
            FlowTask ann,
            Class<?> inferredInputType,
            Class<?> inferredOutputType
    ) {
        Class<?> expectedInput = resolveEffectiveType("input", ann.inputType(), inferredInputType, ann.id());
        Class<?> expectedOutput = resolveEffectiveType("output", ann.outputType(), inferredOutputType, ann.id());
        if (!task.inputType().equals(expectedInput) || !task.outputType().equals(expectedOutput)) {
            throw new WorkflowConfigurationException(
                    "@FlowTask type mismatch for Task bean '" + task.id().getValue() + "': expected("
                            + expectedInput.getName() + " -> " + expectedOutput.getName() + "), actual("
                            + task.inputType().getName() + " -> " + task.outputType().getName() + ")"
            );
        }
        if (!task.id().equals(TaskId.of(ann.id()))) {
            throw new WorkflowConfigurationException(
                    "@FlowTask id mismatch for Task bean: annotation=" + ann.id() + ", task.id=" + task.id().getValue()
            );
        }
    }
}
