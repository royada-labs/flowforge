package io.tugrandsolutions.flowforge.workflow.instance;

import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class WorkflowInstance {

    private final WorkflowExecutionPlan plan;
    private final ReactiveExecutionContext context;

    private final Map<TaskNode, TaskStatus> statusMap =
            new ConcurrentHashMap<>();

    public WorkflowInstance(
            WorkflowExecutionPlan plan,
            ReactiveExecutionContext context
    ) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.context = Objects.requireNonNull(context, "context");

        initialize();
    }

    private void initialize() {
        for (TaskNode node : plan.nodes()) {
            statusMap.put(node, TaskStatus.PENDING);
        }
        for (TaskNode root : plan.roots()) {
            statusMap.put(root, TaskStatus.READY);
        }
    }

    public ReactiveExecutionContext context() {
        return context;
    }

    public TaskStatus status(TaskNode node) {
        return statusMap.get(node);
    }

    public Set<TaskNode> readyTasks() {
        return statusMap.entrySet().stream()
                .filter(e -> e.getValue() == TaskStatus.READY)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void markRunning(TaskNode node) {
        statusMap.put(node, TaskStatus.RUNNING);
    }

    public void markCompleted(TaskNode node) {
        statusMap.put(node, TaskStatus.COMPLETED);
        updateDependents(node);
    }

    public void markFailed(TaskNode node) {
        System.out.println("FAILED: " + node.id() + " optional=" + node.descriptor().optional());
        if (node.descriptor().optional()) {
            // El fallo de una tarea optional se trata como "skipped" para desbloquear el grafo
            markSkipped(node);
            return;
        }
        statusMap.put(node, TaskStatus.FAILED);
        handleFailure(node);
    }

    private void updateDependents(TaskNode completed) {
        for (TaskNode dependent : completed.dependents()) {
            if (canRun(dependent)) {
                statusMap.put(dependent, TaskStatus.READY);
            }
        }
    }

    private boolean canRun(TaskNode node) {
        return node.dependencies().stream()
                .allMatch(dep -> {
                    TaskStatus s = statusMap.get(dep);
                    return s == TaskStatus.COMPLETED || s == TaskStatus.SKIPPED;
                });
    }

    private void handleFailure(TaskNode failed) {
        // aquí SOLO llegan fallos requeridos
        for (TaskNode dependent : failed.dependents()) {
            statusMap.put(dependent, TaskStatus.FAILED);
            // (opcional) si quieres propagación transitiva, llama recursivamente
        }
    }
    public boolean isFinished() {
        return statusMap.values().stream()
                .allMatch(s ->
                        s == TaskStatus.COMPLETED ||
                                s == TaskStatus.FAILED ||
                                s == TaskStatus.SKIPPED
                );
    }

    public void markSkipped(TaskNode node) {
        statusMap.put(node, TaskStatus.SKIPPED);
        updateDependents(node);
    }

    public boolean tryMarkRunning(TaskNode node) {
        TaskStatus updated = statusMap.compute(node, (n, current) -> {
            if (current == TaskStatus.READY) {
                return TaskStatus.RUNNING;
            }
            return current;
        });
        return updated == TaskStatus.RUNNING;
    }
}
