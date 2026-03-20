package org.royada.flowforge.workflow.plan;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskDescriptor;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.validation.TypeMetadata;
import org.royada.flowforge.workflow.graph.WorkflowGraph;

/**
 * Builder for creating {@link WorkflowExecutionPlan} instances.
 */
public final class WorkflowPlanBuilder {

    private WorkflowPlanBuilder() {
        // utility
    }

    /**
     * Builds an execution plan from a collection of tasks.
     * 
     * @param tasks the collection of tasks
     * @return the execution plan
     */
    public static WorkflowExecutionPlan build(Collection<? extends Task<?, ?>> tasks) {
        return build(tasks, Collections.emptyMap());
    }

    /**
     * Builds an execution plan from a collection of tasks and type metadata.
     * 
     * @param tasks the collection of tasks
     * @param typeMetadata the map of type metadata
     * @return the execution plan
     */
    public static WorkflowExecutionPlan build(Collection<? extends Task<?, ?>> tasks, Map<TaskId, TypeMetadata> typeMetadata) {
        Objects.requireNonNull(tasks, "tasks");

        List<TaskDescriptor> descriptors = tasks.stream()
                .map(TaskDescriptor::new)
                .toList();

        return buildFromDescriptors(descriptors, typeMetadata);
    }

    /**
     * Builds an execution plan from a collection of task descriptors and type metadata.
     * 
     * @param descriptors the collection of task descriptors
     * @param typeMetadata the map of type metadata
     * @return the execution plan
     */
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
