package io.tugrandsolutions.flowforge.spring.registry;

import io.tugrandsolutions.flowforge.task.TaskId;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public final class TaskHandlerRegistry {

    private final Map<TaskId, TaskProvider<?, ?>> providers = new ConcurrentHashMap<>();

    public void register(TaskProvider<?, ?> provider) {
        TaskProvider<?, ?> previous = providers.putIfAbsent(provider.id(), provider);
        if (previous != null) {
            throw new IllegalStateException("Duplicate FlowTask id: " + provider.id());
        }
    }

    public Optional<TaskProvider<?, ?>> find(TaskId id) {
        return Optional.ofNullable(providers.get(id));
    }

    public Collection<TaskProvider<?, ?>> snapshot() {
        return Collections.unmodifiableCollection(providers.values());
    }

    public boolean contains(TaskId id) {
        return providers.containsKey(id);
    }
}