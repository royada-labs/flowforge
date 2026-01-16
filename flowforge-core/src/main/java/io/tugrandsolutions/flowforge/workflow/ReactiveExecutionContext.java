package io.tugrandsolutions.flowforge.workflow;

import io.tugrandsolutions.flowforge.task.TaskId;

import java.util.Optional;
import java.util.Set;

public interface ReactiveExecutionContext {

    <T> void put(TaskId taskId, T value);

    <T> Optional<T> get(TaskId taskId, Class<T> type);

    boolean isCompleted(TaskId taskId);

    Set<TaskId> completedTasks();

}