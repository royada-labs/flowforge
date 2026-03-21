package org.royada.flowforge.workflow;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.task.FlowKey;
import org.royada.flowforge.exception.TypeMismatchException;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of {@link ReactiveExecutionContext} that stores task results in memory
 * using a {@link ConcurrentHashMap}.
 */
public class InMemoryReactiveExecutionContext
        implements ReactiveExecutionContext {

    /**
     * Creates a new in-memory reactive execution context.
     */
    public InMemoryReactiveExecutionContext() {}

    private final ConcurrentMap<TaskId, Object> store = new ConcurrentHashMap<>();

    private static final Object NULL_VALUE = new Object();

    @Override
    public <T> void put(FlowKey<T> key, T value) {
        store.put(key.taskId(), value == null ? NULL_VALUE : value);
    }

    @Override
    public <T> Optional<T> get(FlowKey<T> key) {
        Object val = store.get(key.taskId());
        if (val == null) {
            return Optional.empty();
        }
        if (val == NULL_VALUE) {
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
