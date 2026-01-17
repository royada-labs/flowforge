package io.tugrandsolutions.flowforge.workflow.instance;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

public final class WorkflowInstance {

    private final WorkflowRunMetadata metadata;
    private final WorkflowExecutionPlan plan;
    private final ReactiveExecutionContext context;

    // Definitivo: status por ID, no por instancia de TaskNode
    private final Map<TaskId, TaskStatus> statusMap = new ConcurrentHashMap<>();

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

    // Legacy constructor for tests/backwards compatibility (uses "UNKNOWN" or null)
    public WorkflowInstance(WorkflowExecutionPlan plan, ReactiveExecutionContext context) {
        this("UNKNOWN", plan, context);
    }

    private void initialize() {
        for (TaskNode node : plan.nodes()) {
            statusMap.put(node.id(), TaskStatus.PENDING);
        }
        for (TaskNode root : plan.roots()) {
            statusMap.put(root.id(), TaskStatus.READY);
        }
    }

    public WorkflowRunMetadata metadata() {
        return metadata;
    }

    public String workflowId() {
        return metadata.workflowId();
    }

    public ReactiveExecutionContext context() {
        return context;
    }

    public WorkflowExecutionPlan plan() {
        return plan;
    }

    public TaskStatus status(TaskNode node) {
        return statusMap.get(node.id());
    }

    public Set<TaskNode> readyTasks() {
        return plan.nodes().stream()
                .filter(n -> statusMap.get(n.id()) == TaskStatus.READY)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void markRunning(TaskNode node) {
        statusMap.put(node.id(), TaskStatus.RUNNING);
    }

    public void markCompleted(TaskNode node) {
        statusMap.put(node.id(), TaskStatus.COMPLETED);
        updateDependents(node);
    }

    public void markFailed(TaskNode node) {
        // Quita el println (tests no lo necesitan)
        if (node.descriptor().optional()) {
            markSkipped(node);
            return;
        }
        statusMap.put(node.id(), TaskStatus.FAILED);
        handleFailure(node);
    }

    public void markSkipped(TaskNode node) {
        statusMap.put(node.id(), TaskStatus.SKIPPED);
        updateDependents(node);
    }

    public boolean tryMarkRunning(TaskNode node) {
        TaskStatus updated = statusMap.compute(node.id(), (id, current) -> {
            if (current == TaskStatus.READY)
                return TaskStatus.RUNNING;
            return current;
        });
        return updated == TaskStatus.RUNNING;
    }

    private void updateDependents(TaskNode completed) {
        for (TaskNode dependent : completed.dependents()) {
            if (canRun(dependent)) {
                statusMap.put(dependent.id(), TaskStatus.READY);
            }
        }
    }

    private boolean canRun(TaskNode node) {
        return node.dependencies().stream()
                .allMatch(dep -> {
                    TaskStatus s = statusMap.get(dep.id());
                    return s == TaskStatus.COMPLETED || s == TaskStatus.SKIPPED;
                });
    }

    private void handleFailure(TaskNode failed) {
        for (TaskNode dependent : failed.dependents()) {
            statusMap.put(dependent.id(), TaskStatus.FAILED);
        }
    }

    public boolean isFinished() {
        return statusMap.values().stream()
                .allMatch(s -> s == TaskStatus.COMPLETED || s == TaskStatus.FAILED || s == TaskStatus.SKIPPED);
    }
}
