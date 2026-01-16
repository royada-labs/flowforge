package io.tugrandsolutions.flowforge.workflow.graph;

import io.tugrandsolutions.flowforge.task.TaskDescriptor;
import io.tugrandsolutions.flowforge.task.TaskId;

import java.util.*;
import java.util.stream.Collectors;

public final class WorkflowGraph {

    private final Map<TaskId, TaskNode> nodes;

    private WorkflowGraph(Map<TaskId, TaskNode> nodes) {
        this.nodes = Map.copyOf(nodes);
    }

    public static WorkflowGraph build(Collection<TaskDescriptor> tasks) {
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

        return new WorkflowGraph(nodeMap);
    }

    public Collection<TaskNode> nodes() {
        return nodes.values();
    }

    public Optional<TaskNode> get(TaskId taskId) {
        return Optional.ofNullable(nodes.get(taskId));
    }

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