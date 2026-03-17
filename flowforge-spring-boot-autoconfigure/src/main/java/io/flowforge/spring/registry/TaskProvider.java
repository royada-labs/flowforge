package io.flowforge.spring.registry;

import io.flowforge.task.Task;
import io.flowforge.task.TaskId;

public interface TaskProvider {
    TaskId id();
    Task<?, ?> get();
}
