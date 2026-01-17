package io.tugrandsolutions.flowforge.workflow;

import java.util.Optional;
import java.util.Set;

import io.tugrandsolutions.flowforge.task.TaskId;

public interface ReactiveExecutionContext {

    <T> void put(TaskId taskId, T value);

    <T> Optional<T> get(TaskId taskId, Class<T> type);

    default Optional<Object> get(TaskId taskId) {
        return get(taskId, Object.class);
    }

    boolean isCompleted(TaskId taskId);

    Set<TaskId> completedTasks();

}