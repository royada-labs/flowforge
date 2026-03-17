package io.tugrandsolutions.flowforge.workflow;

import io.tugrandsolutions.flowforge.task.TaskId;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryReactiveExecutionContext
        implements ReactiveExecutionContext {

    private final ConcurrentMap<TaskId, Object> store = new ConcurrentHashMap<>();

    @Override
    public <T> void put(TaskId taskId, T value) {
        store.put(taskId, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(TaskId taskId, Class<T> type) {
        return Optional.ofNullable(store.get(taskId))
                .filter(v -> v == null || type.isInstance(v))
                .map(v -> (T) v);
    }

    @Override
    public Optional<Object> get(TaskId taskId) {
        return Optional.ofNullable(store.get(taskId));
    }

    @Override
    public boolean isCompleted(TaskId taskId) {
        return store.containsKey(taskId);
    }

    @Override
    public Set<TaskId> completedTasks() {
        return Set.copyOf(store.keySet());
    }
}