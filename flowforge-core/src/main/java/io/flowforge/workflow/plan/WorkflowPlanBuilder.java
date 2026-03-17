package io.flowforge.workflow.plan;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.flowforge.task.Task;
import io.flowforge.task.TaskDescriptor;
import io.flowforge.workflow.graph.WorkflowGraph;
import io.flowforge.validation.TypeMetadata;

public final class WorkflowPlanBuilder {

    private WorkflowPlanBuilder() {
        // utility
    }

    public static WorkflowExecutionPlan build(Collection<? extends Task<?, ?>> tasks) {
        return build(tasks, Collections.emptyMap());
    }

    public static WorkflowExecutionPlan build(Collection<? extends Task<?, ?>> tasks, Map<String, TypeMetadata> typeMetadata) {
        Objects.requireNonNull(tasks, "tasks");

        List<TaskDescriptor> descriptors = tasks.stream()
                .map(TaskDescriptor::new)
                .toList();

        // Validate plan before building graph
        WorkflowPlanValidator.validate(descriptors);

        WorkflowGraph graph = WorkflowGraph.build(descriptors, typeMetadata);

        return WorkflowExecutionPlan.from(graph);
    }
}