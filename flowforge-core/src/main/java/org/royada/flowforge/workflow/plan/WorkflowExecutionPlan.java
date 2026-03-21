package org.royada.flowforge.workflow.plan;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.graph.WorkflowGraph;
import org.royada.flowforge.validation.TypeMetadata;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a plan for executing a workflow, derived from its graph.
 */
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

    /**
     * Creates an execution plan from the given workflow graph.
     * 
     * @param graph the workflow graph
     * @return the execution plan
     */
    public static WorkflowExecutionPlan from(WorkflowGraph graph) {
        Objects.requireNonNull(graph, "graph");
        return new WorkflowExecutionPlan(
                graph,
                graph.roots()
        );
    }

    /**
     * Returns the root nodes of the workflow.
     * 
     * @return the set of root nodes
     */
    public Set<TaskNode> roots() {
        return roots;
    }

    /**
     * Returns the task node for the given task ID, if it exists.
     * 
     * @param taskId the task ID
     * @return an optional containing the node, or empty if not found
     */
    public Optional<TaskNode> getNode(TaskId taskId) {
        return graph.get(taskId);
    }

    /**
     * Returns all task nodes in the plan.
     * 
     * @return the collection of nodes
     */
    public Collection<TaskNode> nodes() {
        return graph.nodes();
    }

    /**
     * Returns the type metadata for all tasks in the plan.
     * 
     * @return the map of type metadata
     */
    public Map<TaskId, TypeMetadata> typeMetadata() {
        return graph.typeMetadata();
    }
}
