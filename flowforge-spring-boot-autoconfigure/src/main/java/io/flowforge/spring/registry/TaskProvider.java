package io.flowforge.spring.registry;

import io.flowforge.task.Task;
import io.flowforge.task.TaskId;

public interface TaskProvider<I, O> {
    TaskId id();
    Task<I, O> get(); // devuelve una instancia nueva o la que decidas
}