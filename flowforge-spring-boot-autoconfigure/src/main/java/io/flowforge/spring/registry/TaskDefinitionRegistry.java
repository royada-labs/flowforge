package io.flowforge.spring.registry;

import io.flowforge.exception.TaskRegistrationException;
import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;

import java.util.Map;
import java.util.Objects;
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
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(beanName, "beanName");
        putOrVerify(byId, definition.id(), definition, "task id");
        putOrVerify(byBeanName, beanName, definition, "bean name");
    }

    public void registerMethodRef(
            String implClassInternalName,
            String methodName,
            String methodDescriptor,
            TaskDefinition<?, ?> definition
    ) {
        Objects.requireNonNull(definition, "definition");
        String key = methodRefKey(implClassInternalName, methodName, methodDescriptor);
        putOrVerify(byMethodRef, key, definition, "method reference");
    }

    public Optional<TaskDefinition<?, ?>> find(TaskId id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<TaskDefinition<?, ?>> findByBeanName(String beanName) {
        return Optional.ofNullable(byBeanName.get(beanName));
    }

    public Optional<TaskDefinition<?, ?>> findByMethodRef(
            String implClassInternalName,
            String methodName,
            String methodDescriptor
    ) {
        return Optional.ofNullable(byMethodRef.get(methodRefKey(implClassInternalName, methodName, methodDescriptor)));
    }

    private static String methodRefKey(String implClassInternalName, String methodName, String methodDescriptor) {
        Objects.requireNonNull(implClassInternalName, "implClassInternalName");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(methodDescriptor, "methodDescriptor");
        return implClassInternalName + "#" + methodName + "#" + methodDescriptor;
    }

    private static <K> void putOrVerify(
            Map<K, TaskDefinition<?, ?>> map,
            K key,
            TaskDefinition<?, ?> definition,
            String keyType
    ) {
        TaskDefinition<?, ?> previous = map.putIfAbsent(key, definition);
        if (previous != null && !previous.equals(definition)) {
            throw new TaskRegistrationException(
                    "Conflicting TaskDefinition for " + keyType + " '" + key + "': " + previous + " vs " + definition
            );
        }
    }
}
