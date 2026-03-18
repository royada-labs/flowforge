package io.flowforge.task;

import io.flowforge.workflow.policy.ExecutionPolicy;

import java.util.Objects;
import java.util.Set;

public final class TaskDescriptor {

    private final Task<?, ?> task;
    private final ExecutionPolicy policy;

    public TaskDescriptor(Task<?, ?> task) {
        this(task, ExecutionPolicy.defaultPolicy());
    }

    public TaskDescriptor(Task<?, ?> task, ExecutionPolicy policy) {
        this.task = Objects.requireNonNull(task, "task");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public TaskId id() {
        return task.id();
    }

    public Set<TaskId> dependencies() {
        return task.dependencies();
    }

    public boolean optional() {
        return task.optional();
    }

    public Task<?, ?> task() {
        return task;
    }

    public ExecutionPolicy policy() {
        return policy;
    }
}