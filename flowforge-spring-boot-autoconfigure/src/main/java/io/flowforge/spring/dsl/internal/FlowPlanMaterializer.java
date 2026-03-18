package io.flowforge.spring.dsl.internal;

import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.spring.registry.TaskProvider;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.*;

public final class FlowPlanMaterializer {

    private final TaskHandlerRegistry registry;

    public FlowPlanMaterializer(TaskHandlerRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public WorkflowExecutionPlan materialize(FlowGraph graph) {
        Objects.requireNonNull(graph, "graph");

        // 1) validate tasks exist
        Map<TaskId, Task<?, ?>> baseTasksById = new LinkedHashMap<>();
        for (TaskId taskId : graph.nodes()) {
            TaskProvider provider = registry.find(taskId)
                    .orElseThrow(() -> new IllegalStateException("Unknown task id: " + taskId.getValue()));
            baseTasksById.put(taskId, provider.get());
        }

        // 2) compute workflow dependencies from edges
        Map<TaskId, Set<TaskId>> deps = new LinkedHashMap<>();
        for (TaskId id : graph.nodes()) {
            deps.put(id, new LinkedHashSet<>());
        }
        for (Edge e : graph.edges()) {
            deps.get(e.to()).add(e.from());
        }

        // 3) wrap tasks with workflow-scoped deps
        List<Task<?, ?>> scopedTasks = new ArrayList<>();
        for (Map.Entry<TaskId, Task<?, ?>> entry : baseTasksById.entrySet()) {
            scopedTasks.add(wrap(entry.getValue(), deps.get(entry.getKey())));
        }

        // 4) Build plan using core builder
        return io.flowforge.workflow.plan.WorkflowPlanBuilder.build(scopedTasks, graph.typeMetadata());
    }

    private static Task<?, ?> wrap(Task<?, ?> base, Set<TaskId> deps) {
        return wrapTyped(base, deps);
    }

    private static <I, O> Task<I, O> wrapTyped(Task<I, O> base, Set<TaskId> deps) {
        return new WorkflowScopedTask<>(base, deps);
    }
}
