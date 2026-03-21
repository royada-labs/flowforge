package org.royada.flowforge.spring.registry;

import org.royada.flowforge.exception.TaskRegistrationException;
import org.royada.flowforge.task.TaskId;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * In-memory registry of {@link TaskProvider} instances keyed by {@link TaskId}.
 */
public final class TaskHandlerRegistry {

    private final Map<TaskId, TaskProvider> providers = new ConcurrentHashMap<>();

    /**
     * Creates an empty task handler registry.
     */
    public TaskHandlerRegistry() {
    }

    /**
     * Registers a task provider.
     *
     * @param provider provider to register
     */
    public void register(TaskProvider provider) {
        TaskProvider previous = providers.putIfAbsent(provider.id(), provider);
        if (previous != null) {
            throw new TaskRegistrationException("Duplicate FlowTask id: " + provider.id());
        }
    }

    /**
     * Finds a provider by task id.
     *
     * @param id task id
     * @return provider if present
     */
    public Optional<TaskProvider> find(TaskId id) {
        return Optional.ofNullable(providers.get(id));
    }

    /**
     * Returns an immutable snapshot of current providers.
     *
     * @return registered providers
     */
    public Collection<TaskProvider> snapshot() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * Checks whether a provider exists for the given id.
     *
     * @param id task id
     * @return {@code true} when present
     */
    public boolean contains(TaskId id) {
        return providers.containsKey(id);
    }
}
