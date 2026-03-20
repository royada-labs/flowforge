package org.royada.flowforge.spring.registry;

import org.royada.flowforge.exception.TaskRegistrationException;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.task.TaskId;

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

    /**
     * Creates an empty registry.
     */
    public TaskDefinitionRegistry() {
    }

    /**
     * Registers a definition by task id and bean name.
     *
     * @param definition task definition
     * @param beanName spring bean name owning the definition
     */
    public void register(TaskDefinition<?, ?> definition, String beanName) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(beanName, "beanName");
        putOrVerify(byId, definition.id(), definition, "task id");
        putOrVerify(byBeanName, beanName, definition, "bean name");
    }

    /**
     * Registers a definition resolved from a serialized method reference key.
     *
     * @param implClassInternalName internal JVM class name
     * @param methodName method name
     * @param methodDescriptor JVM method descriptor
     * @param definition task definition
     */
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

    /**
     * Finds a task definition by task id.
     *
     * @param id task id
     * @return definition if present
     */
    public Optional<TaskDefinition<?, ?>> find(TaskId id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Finds a task definition by Spring bean name.
     *
     * @param beanName spring bean name
     * @return definition if present
     */
    public Optional<TaskDefinition<?, ?>> findByBeanName(String beanName) {
        return Optional.ofNullable(byBeanName.get(beanName));
    }

    /**
     * Finds a task definition by serialized method reference key components.
     *
     * @param implClassInternalName internal JVM class name
     * @param methodName method name
     * @param methodDescriptor JVM method descriptor
     * @return definition if present
     */
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
