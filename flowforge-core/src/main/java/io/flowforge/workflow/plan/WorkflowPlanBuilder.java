package io.flowforge.workflow.plan;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.flowforge.task.Task;
import io.flowforge.task.TaskDescriptor;
import io.flowforge.task.TaskId;
import io.flowforge.validation.TypeMetadata;
import io.flowforge.workflow.graph.WorkflowGraph;

public final class WorkflowPlanBuilder {

    private WorkflowPlanBuilder() {
        // utility
    }

    public static WorkflowExecutionPlan build(Collection<? extends Task<?, ?>> tasks) {
        return build(tasks, Collections.emptyMap());
    }

    public static WorkflowExecutionPlan build(Collection<? extends Task<?, ?>> tasks, Map<TaskId, TypeMetadata> typeMetadata) {
        Objects.requireNonNull(tasks, "tasks");

        List<TaskDescriptor> descriptors = tasks.stream()
                .map(TaskDescriptor::new)
                .toList();

        return buildFromDescriptors(descriptors, typeMetadata);
    }

    public static WorkflowExecutionPlan buildFromDescriptors(
            Collection<TaskDescriptor> descriptors,
            Map<TaskId, TypeMetadata> typeMetadata
    ) {
        Objects.requireNonNull(descriptors, "descriptors");
        List<TaskDescriptor> descriptorList = List.copyOf(descriptors);

        // Validate plan before building graph
        WorkflowPlanValidator.validate(descriptorList);

        WorkflowGraph graph = WorkflowGraph.build(descriptorList, typeMetadata);

        return WorkflowExecutionPlan.from(graph);
    }
}
