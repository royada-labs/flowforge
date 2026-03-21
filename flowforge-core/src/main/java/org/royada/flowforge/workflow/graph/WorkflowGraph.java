package org.royada.flowforge.workflow.graph;

import org.royada.flowforge.task.TaskDescriptor;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.validation.TypeMetadata;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the dependency graph of tasks in a workflow.
 */
public final class WorkflowGraph {

    private final Map<TaskId, TaskNode> nodes;
    private final Map<TaskId, TypeMetadata> typeMetadata;

    private WorkflowGraph(Map<TaskId, TaskNode> nodes, Map<TaskId, TypeMetadata> typeMetadata) {
        this.nodes = Map.copyOf(nodes);
        this.typeMetadata = Map.copyOf(typeMetadata != null ? typeMetadata : Collections.emptyMap());
    }

    /**
     * Builds a workflow graph from a collection of task descriptors.
     * 
     * @param tasks the collection of task descriptors
     * @return the built workflow graph
     * @throws IllegalArgumentException if duplicate task IDs are found
     * @throws IllegalStateException if a task depends on a missing task or if a cycle is detected
     */
    public static WorkflowGraph build(Collection<TaskDescriptor> tasks) {
        return build(tasks, Collections.emptyMap());
    }

    /**
     * Builds a workflow graph from a collection of task descriptors and type metadata.
     * 
     * @param tasks the collection of task descriptors
     * @param typeMetadata the map of type metadata for the tasks
     * @return the built workflow graph
     * @throws IllegalArgumentException if duplicate task IDs are found
     * @throws IllegalStateException if a task depends on a missing task or if a cycle is detected
     */
    public static WorkflowGraph build(Collection<TaskDescriptor> tasks, Map<TaskId, TypeMetadata> typeMetadata) {
        Objects.requireNonNull(tasks, "tasks");

        Map<TaskId, TaskNode> nodeMap = new HashMap<>();

        // 1. Crear nodos
        for (TaskDescriptor descriptor : tasks) {
            TaskId id = descriptor.id();
            if (nodeMap.containsKey(id)) {
                throw new IllegalArgumentException(
                        "Duplicate task id detected: " + id
                );
            }
            nodeMap.put(id, new TaskNode(descriptor));
        }

        // 2. Resolver dependencias
        for (TaskNode node : nodeMap.values()) {
            for (TaskId dependencyId : node.descriptor().dependencies()) {
                TaskNode dependency = nodeMap.get(dependencyId);
                if (dependency == null) {
                    throw new IllegalStateException(
                            "Task " + node.id() +
                                    " depends on missing task " + dependencyId
                    );
                }
                node.addDependency(dependency);
                dependency.addDependent(node);
            }
        }

        // 3. Validar ciclos
        detectCycles(nodeMap.values());

        return new WorkflowGraph(nodeMap, typeMetadata);
    }

    /**
     * Returns the type metadata for the tasks in the graph.
     * 
     * @return the map of type metadata
     */
    public Map<TaskId, TypeMetadata> typeMetadata() {
        return typeMetadata;
    }

    /**
     * Returns all task nodes in the graph.
     * 
     * @return the collection of nodes
     */
    public Collection<TaskNode> nodes() {
        return nodes.values();
    }

    /**
     * Returns the task node for the given task ID, if it exists.
     * 
     * @param taskId the task ID
     * @return an optional containing the node, or empty if not found
     */
    public Optional<TaskNode> get(TaskId taskId) {
        return Optional.ofNullable(nodes.get(taskId));
    }

    /**
     * Returns the root nodes of the graph (those with no dependencies).
     * 
     * @return an unmodifiable set of root nodes
     */
    public Set<TaskNode> roots() {
        return nodes.values().stream()
                .filter(TaskNode::isRoot)
                .collect(Collectors.toUnmodifiableSet());
    }

    /* ======================= */
    /* === Cycle detection === */
    /* ======================= */

    private static void detectCycles(Collection<TaskNode> nodes) {
        Set<TaskNode> visiting = new HashSet<>();
        Set<TaskNode> visited = new HashSet<>();

        for (TaskNode node : nodes) {
            if (!visited.contains(node)) {
                dfs(node, visiting, visited);
            }
        }
    }

    private static void dfs(
            TaskNode node,
            Set<TaskNode> visiting,
            Set<TaskNode> visited
    ) {
        if (visiting.contains(node)) {
            throw new IllegalStateException(
                    "Cycle detected involving task " + node.id()
            );
        }

        if (visited.contains(node)) {
            return;
        }

        visiting.add(node);
        for (TaskNode dependency : node.dependencies()) {
            dfs(dependency, visiting, visited);
        }
        visiting.remove(node);
        visited.add(node);
    }
}
