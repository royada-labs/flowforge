package io.tugrandsolutions.flowforge.spring.registry;

import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;

public interface TaskProvider<I, O> {
    TaskId id();
    Task<I, O> get(); // devuelve una instancia nueva o la que decidas
}