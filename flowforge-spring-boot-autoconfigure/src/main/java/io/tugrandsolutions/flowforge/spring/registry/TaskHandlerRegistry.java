package io.tugrandsolutions.flowforge.spring.registry;

import io.tugrandsolutions.flowforge.task.TaskId;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import io.tugrandsolutions.flowforge.task.Task;

public final class TaskHandlerRegistry {

    private final Map<TaskId, Task<?, ?>> tasks =
            new ConcurrentHashMap<>();

    public void register(Task<?, ?> task) {
        Task<?, ?> previous = tasks.putIfAbsent(task.id(), task);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate FlowTask id: " + task.id()
            );
        }
    }

    public Optional<Task<?, ?>> find(TaskId id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Collection<Task<?, ?>> snapshot() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public boolean contains(TaskId id) {
        return tasks.containsKey(id);
    }
}