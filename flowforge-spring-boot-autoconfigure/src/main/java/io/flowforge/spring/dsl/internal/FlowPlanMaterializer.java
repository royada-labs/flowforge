package io.flowforge.spring.dsl.internal;

import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.spring.registry.TaskProvider;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.*;
import java.util.stream.Collectors;

public final class FlowPlanMaterializer {

    private final TaskHandlerRegistry registry;

    public FlowPlanMaterializer(TaskHandlerRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public WorkflowExecutionPlan materialize(FlowGraph graph) {
        Objects.requireNonNull(graph, "graph");

        // 1) validate tasks exist
        Map<String, Task<?, ?>> baseTasksById = new LinkedHashMap<>();
        for (String id : graph.nodes()) {
            TaskId taskId = new TaskId(id);
            TaskProvider<?, ?> provider = registry.find(taskId)
                    .orElseThrow(() -> new IllegalStateException("Unknown task id: " + id));
            baseTasksById.put(id, provider.get());
        }

        // 2) compute workflow dependencies from edges
        Map<String, Set<TaskId>> deps = new LinkedHashMap<>();
        for (String id : graph.nodes()) {
            deps.put(id, new LinkedHashSet<>());
        }
        for (Edge e : graph.edges()) {
            deps.get(e.to()).add(new TaskId(e.from()));
        }

        // 3) wrap tasks with workflow-scoped deps
        List<Task<?, ?>> scopedTasks = baseTasksById.entrySet().stream()
                .map(entry -> wrap(entry.getValue(), deps.get(entry.getKey())))
                .collect(Collectors.toList());

        // 4) Build plan using core builder
        return io.flowforge.workflow.plan.WorkflowPlanBuilder.build(scopedTasks, graph.typeMetadata());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Task<?, ?> wrap(Task<?, ?> base, Set<TaskId> deps) {
        return new WorkflowScopedTask((Task) base, deps);
    }
}