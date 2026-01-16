package io.tugrandsolutions.flowforge.workflow.plan;

import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.graph.WorkflowGraph;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WorkflowExecutionPlan {

    private final WorkflowGraph graph;
    private final Set<TaskNode> roots;

    private WorkflowExecutionPlan(
            WorkflowGraph graph,
            Set<TaskNode> roots
    ) {
        this.graph = graph;
        this.roots = roots;
    }

    public static WorkflowExecutionPlan from(WorkflowGraph graph) {
        Objects.requireNonNull(graph, "graph");
        return new WorkflowExecutionPlan(
                graph,
                graph.roots()
        );
    }

    public Set<TaskNode> roots() {
        return roots;
    }

    public Optional<TaskNode> getNode(TaskId taskId) {
        return graph.get(taskId);
    }

    public Collection<TaskNode> nodes() {
        return graph.nodes();
    }
}