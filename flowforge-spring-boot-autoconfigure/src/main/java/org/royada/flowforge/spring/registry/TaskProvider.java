package org.royada.flowforge.spring.registry;

import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.policy.ExecutionPolicy;

/**
 * Lazily resolves executable task instances and optional execution policy metadata.
 */
public interface TaskProvider {
    /**
     * Returns the unique task id provided by this provider.
     *
     * @return task id
     */
    TaskId id();

    /**
     * Resolves an executable task instance.
     *
     * @return task instance
     */
    Task<?, ?> get();

    /**
     * Returns the provider-level execution policy applied to the task.
     *
     * @return execution policy
     */
    default ExecutionPolicy policy() {
        return ExecutionPolicy.defaultPolicy();
    }
}
