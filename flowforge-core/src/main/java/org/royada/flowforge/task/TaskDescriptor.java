package org.royada.flowforge.task;

import org.royada.flowforge.workflow.policy.ExecutionPolicy;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a task for execution, including its associated policy.
 */
public final class TaskDescriptor {

    private final Task<?, ?> task;
    private final ExecutionPolicy policy;

    /**
     * Creates a new descriptor with default execution policy.
     * 
     * @param task the task to describe
     */
    public TaskDescriptor(Task<?, ?> task) {
        this(task, ExecutionPolicy.defaultPolicy());
    }

    /**
     * Creates a new descriptor with a custom execution policy.
     * 
     * @param task the task to describe
     * @param policy the execution policy
     */
    public TaskDescriptor(Task<?, ?> task, ExecutionPolicy policy) {
        this.task = Objects.requireNonNull(task, "task");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * Returns the ID of the task.
     * 
     * @return the task ID
     */
    public TaskId id() {
        return task.id();
    }

    /**
     * Returns the IDs of the tasks that this task depends on.
     * 
     * @return the set of dependency IDs
     */
    public Set<TaskId> dependencies() {
        return task.dependencies();
    }

    /**
     * Returns whether the task is optional.
     * 
     * @return {@code true} if the task is optional, {@code false} otherwise
     */
    public boolean optional() {
        return task.optional();
    }

    /**
     * Returns the underlying task.
     * 
     * @return the task
     */
    public Task<?, ?> task() {
        return task;
    }

    /**
     * Returns the execution policy for the task.
     * 
     * @return the policy
     */
    public ExecutionPolicy policy() {
        return policy;
    }
}