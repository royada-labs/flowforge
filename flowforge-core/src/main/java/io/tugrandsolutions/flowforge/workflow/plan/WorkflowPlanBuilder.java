package io.tugrandsolutions.flowforge.workflow.plan;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskDescriptor;
import io.tugrandsolutions.flowforge.workflow.graph.WorkflowGraph;

public final class WorkflowPlanBuilder {

    private WorkflowPlanBuilder() {
        // utility
    }

    public static WorkflowExecutionPlan build(Collection<? extends Task<?, ?>> tasks) {
        Objects.requireNonNull(tasks, "tasks");

        List<TaskDescriptor> descriptors = tasks.stream()
                .map(TaskDescriptor::new)
                .toList();

        // Validate plan before building graph
        WorkflowPlanValidator.validate(descriptors);

        WorkflowGraph graph = WorkflowGraph.build(descriptors);

        return WorkflowExecutionPlan.from(graph);
    }
}