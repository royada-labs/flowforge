package io.flowforge.spring.registry;

import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.policy.ExecutionPolicy;

public interface TaskProvider {
    TaskId id();
    Task<?, ?> get();

    default ExecutionPolicy policy() {
        return ExecutionPolicy.defaultPolicy();
    }
}
