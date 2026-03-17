package io.flowforge.spring.registry;

import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Start-time catalog of typed task metadata used by DSL reference resolution.
 */
public final class TaskDefinitionRegistry {

    private final Map<TaskId, TaskDefinition<?, ?>> byId = new ConcurrentHashMap<>();
    private final Map<String, TaskDefinition<?, ?>> byBeanName = new ConcurrentHashMap<>();
    private final Map<String, TaskDefinition<?, ?>> byMethodRef = new ConcurrentHashMap<>();

    public void register(TaskDefinition<?, ?> definition, String beanName) {
        byId.put(definition.id(), definition);
        byBeanName.put(beanName, definition);
    }

    public void registerMethodRef(String implClassInternalName, String methodName, TaskDefinition<?, ?> definition) {
        byMethodRef.put(methodRefKey(implClassInternalName, methodName), definition);
    }

    public Optional<TaskDefinition<?, ?>> find(TaskId id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<TaskDefinition<?, ?>> findByBeanName(String beanName) {
        return Optional.ofNullable(byBeanName.get(beanName));
    }

    public Optional<TaskDefinition<?, ?>> findByMethodRef(String implClassInternalName, String methodName) {
        return Optional.ofNullable(byMethodRef.get(methodRefKey(implClassInternalName, methodName)));
    }

    private static String methodRefKey(String implClassInternalName, String methodName) {
        return implClassInternalName + "#" + methodName;
    }
}
