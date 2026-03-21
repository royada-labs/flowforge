package org.royada.flowforge.workflow.instance;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Tracks the state of a single workflow DAG execution.
 * Optimized for O(1) task activation using dependency counters.
 */
public final class WorkflowInstance {

    private final WorkflowRunMetadata metadata;
    private final WorkflowExecutionPlan plan;
    private final ReactiveExecutionContext context;

    private final Map<TaskId, TaskStatus> statusMap = new ConcurrentHashMap<>();
    private final Map<TaskId, AtomicInteger> remainingDependencies = new ConcurrentHashMap<>();

    // Thread-safe set of tasks that are ready to run (O(1) access)
    private final Set<TaskId> readyTaskIds = ConcurrentHashMap.newKeySet();

    // Cached finished state for O(1) reads
    private final AtomicBoolean finishedCache = new AtomicBoolean(false);

    /**
     * Creates a new workflow instance with the given metadata, plan, and context.
     * 
     * @param metadata the run metadata
     * @param plan the execution plan
     * @param context the execution context
     */
    public WorkflowInstance(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            ReactiveExecutionContext context) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.context = Objects.requireNonNull(context, "context");
        initialize();
    }

    /**
     * Creates a new workflow instance with the given workflow ID, plan, and context.
     * 
     * @param workflowId the workflow ID
     * @param plan the execution plan
     * @param context the execution context
     */
    public WorkflowInstance(String workflowId, WorkflowExecutionPlan plan, ReactiveExecutionContext context) {
        this(WorkflowRunMetadata.of(workflowId), plan, context);
    }

    /**
     * Creates a new workflow instance with default metadata.
     * 
     * @param plan the execution plan
     * @param context the execution context
     */
    public WorkflowInstance(WorkflowExecutionPlan plan, ReactiveExecutionContext context) {
        this("UNKNOWN", plan, context);
    }

    private void initialize() {
        for (TaskNode node : plan.nodes()) {
            statusMap.put(node.id(), TaskStatus.PENDING);
            int dependencyCount = node.dependencies().size();
            remainingDependencies.put(node.id(), new AtomicInteger(dependencyCount));
            
            if (dependencyCount == 0) {
                statusMap.put(node.id(), TaskStatus.READY);
                readyTaskIds.add(node.id());
            }
        }
        updateFinishedCache();
    }

    /**
     * Returns the run metadata.
     * 
     * @return the metadata
     */
    public WorkflowRunMetadata metadata() { return metadata; }

    /**
     * Returns the workflow ID.
     * 
     * @return the workflow ID
     */
    public String workflowId() { return metadata.workflowId(); }

    /**
     * Returns the reactive execution context.
     * 
     * @return the context
     */
    public ReactiveExecutionContext context() { return context; }

    /**
     * Returns the execution plan.
     * 
     * @return the plan
     */
    public WorkflowExecutionPlan plan() { return plan; }

    /**
     * Returns the status of a specific task node.
     * 
     * @param node the task node
     * @return the current status of the task
     */
    public TaskStatus status(TaskNode node) {
        return statusMap.get(node.id());
    }

    /**
     * Returns the set of tasks that are ready to be executed (O(1) access).
     * 
     * @return the set of ready task nodes
     */
    public Set<TaskNode> readyTasks() {
        return readyTaskIds.stream()
                .map(id -> plan.getNode(id).orElseThrow())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Attempts to mark a task as running.
     * 
     * @param node the task node to mark
     * @return {@code true} if the task was in READY status and was successfully marked as RUNNING, {@code false} otherwise
     */
    public boolean tryMarkRunning(TaskNode node) {
        TaskStatus updated = statusMap.compute(node.id(), (id, current) -> {
            if (current == TaskStatus.READY) {
                readyTaskIds.remove(id);
                return TaskStatus.RUNNING;
            }
            return current;
        });
        return updated == TaskStatus.RUNNING;
    }

    /**
     * Marks a task as successfully completed.
     * 
     * @param node the completed task node
     */
    public void markCompleted(TaskNode node) {
        statusMap.put(node.id(), TaskStatus.COMPLETED);
        decrementDependents(node);
        updateFinishedCache();
    }

    /**
     * Marks a task as failed. If the task is optional, it will be marked as skipped instead.
     * 
     * @param node the failed task node
     */
    public void markFailed(TaskNode node) {
        if (node.descriptor().optional()) {
            markSkipped(node);
            return;
        }
        statusMap.put(node.id(), TaskStatus.FAILED);
        failDownstream(node);
        updateFinishedCache();
    }

    /**
     * Marks a task as skipped.
     * 
     * @param node the skipped task node
     */
    public void markSkipped(TaskNode node) {
        statusMap.put(node.id(), TaskStatus.SKIPPED);
        decrementDependents(node);
        updateFinishedCache();
    }

    private void decrementDependents(TaskNode completed) {
        for (TaskNode dependent : completed.dependents()) {
            AtomicInteger remaining = remainingDependencies.get(dependent.id());
            if (remaining != null && remaining.decrementAndGet() == 0) {
                statusMap.compute(dependent.id(), (id, current) -> {
                    if (current == TaskStatus.PENDING) {
                        readyTaskIds.add(id);
                        return TaskStatus.READY;
                    }
                    return current;
                });
            }
        }
    }

    private void failDownstream(TaskNode failed) {
        for (TaskNode dependent : failed.dependents()) {
            statusMap.compute(dependent.id(), (id, current) -> {
                if (current == TaskStatus.PENDING || current == TaskStatus.READY) {
                    readyTaskIds.remove(id);
                    failDownstream(dependent);
                    return TaskStatus.FAILED;
                }
                return current;
            });
        }
    }

    /**
     * Returns whether the workflow execution has finished.
     * 
     * @return {@code true} if finished, {@code false} otherwise
     */
    public boolean isFinished() {
        return finishedCache.get();
    }

    private void updateFinishedCache() {
        boolean finished = statusMap.values().stream()
                .allMatch(s -> s == TaskStatus.COMPLETED 
                              || s == TaskStatus.FAILED 
                              || s == TaskStatus.SKIPPED);
        finishedCache.set(finished);
    }
}
