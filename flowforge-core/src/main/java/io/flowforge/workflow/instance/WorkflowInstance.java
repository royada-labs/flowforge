package io.flowforge.workflow.instance;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

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

    public WorkflowInstance(WorkflowRunMetadata metadata, WorkflowExecutionPlan plan,
            ReactiveExecutionContext context) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.context = Objects.requireNonNull(context, "context");
        initialize();
    }

    public WorkflowInstance(String workflowId, WorkflowExecutionPlan plan, ReactiveExecutionContext context) {
        this(WorkflowRunMetadata.of(workflowId), plan, context);
    }

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
    }

    public WorkflowRunMetadata metadata() { return metadata; }
    public String workflowId() { return metadata.workflowId(); }
    public ReactiveExecutionContext context() { return context; }
    public WorkflowExecutionPlan plan() { return plan; }

    public TaskStatus status(TaskNode node) {
        return statusMap.get(node.id());
    }

    /**
     * O(1) access to ready tasks.
     */
    public Set<TaskNode> readyTasks() {
        return readyTaskIds.stream()
                .map(id -> plan.getNode(id).orElseThrow())
                .collect(Collectors.toUnmodifiableSet());
    }

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

    public void markCompleted(TaskNode node) {
        statusMap.put(node.id(), TaskStatus.COMPLETED);
        decrementDependents(node);
    }

    public void markFailed(TaskNode node) {
        if (node.descriptor().optional()) {
            markSkipped(node);
            return;
        }
        statusMap.put(node.id(), TaskStatus.FAILED);
        // Cascade failure to ALL downstream dependents immediately
        failDownstream(node);
    }

    public void markSkipped(TaskNode node) {
        statusMap.put(node.id(), TaskStatus.SKIPPED);
        decrementDependents(node);
    }

    private void decrementDependents(TaskNode completed) {
        for (TaskNode dependent : completed.dependents()) {
            AtomicInteger remaining = remainingDependencies.get(dependent.id());
            if (remaining != null && remaining.decrementAndGet() == 0) {
                // All dependencies met
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
                    // Recursively fail
                    failDownstream(dependent);
                    return TaskStatus.FAILED;
                }
                return current;
            });
        }
    }

    public boolean isFinished() {
        return statusMap.values().stream()
                .allMatch(s -> s == TaskStatus.COMPLETED || s == TaskStatus.FAILED || s == TaskStatus.SKIPPED);
    }
}
