package org.royada.flowforge.workflow;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.task.FlowKey;
import org.royada.flowforge.exception.TypeMismatchException;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryReactiveExecutionContext
        implements ReactiveExecutionContext {

    private final ConcurrentMap<TaskId, Object> store = new ConcurrentHashMap<>();

    @Override
    public <T> void put(FlowKey<T> key, T value) {
        store.put(key.taskId(), value);
    }

    @Override
    public <T> Optional<T> get(FlowKey<T> key) {
        Object val = store.get(key.taskId());
        if (val == null) {
            return Optional.empty();
        }
        if (!key.type().isInstance(val)) {
            throw new TypeMismatchException(key.taskId(), key.type(), val.getClass());
        }
        return Optional.of(key.type().cast(val));
    }

    @Override
    public boolean isCompleted(FlowKey<?> key) {
        return store.containsKey(key.taskId());
    }

    @Override
    public Set<TaskId> completedTasks() {
        return Set.copyOf(store.keySet());
    }
}
