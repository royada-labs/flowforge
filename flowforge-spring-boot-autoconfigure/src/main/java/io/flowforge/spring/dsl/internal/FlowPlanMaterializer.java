package io.flowforge.spring.dsl.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.spring.registry.TaskProvider;
import io.flowforge.task.Task;
import io.flowforge.task.TaskDescriptor;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.policy.ExecutionPolicy;

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

        // 3) wrap tasks with workflow-scoped deps and effective policies
        List<TaskDescriptor> descriptors = new ArrayList<>();
        for (Map.Entry<TaskId, Task<?, ?>> entry : baseTasksById.entrySet()) {
            TaskId taskId = entry.getKey();
            TaskProvider provider = registry.find(taskId)
                    .orElseThrow(() -> new IllegalStateException("Unknown task id: " + taskId.getValue()));

            Task<?, ?> scopedTask = wrap(entry.getValue(), deps.get(taskId));

            ExecutionPolicy effective = provider.policy();
            Optional<ExecutionPolicy> flowPolicy = graph.policy(taskId);
            if (flowPolicy.isPresent()) {
                effective = effective.andThen(flowPolicy.get());
            }

            descriptors.add(new TaskDescriptor(scopedTask, effective));
        }

        // 4) Build plan using core builder
        return io.flowforge.workflow.plan.WorkflowPlanBuilder.buildFromDescriptors(descriptors, graph.typeMetadata());
    }

    private static Task<?, ?> wrap(Task<?, ?> base, Set<TaskId> deps) {
        return wrapTyped(base, deps);
    }

    private static <I, O> Task<I, O> wrapTyped(Task<I, O> base, Set<TaskId> deps) {
        return new WorkflowScopedTask<>(base, deps);
    }
}
