package org.royada.flowforge.workflow.input;

import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
import reactor.core.publisher.Mono;

/**
 * Strategy for resolving runtime input for a task node.
 */
public interface TaskInputResolver {

    /**
     * Resolves the input to pass into a task execution.
     *
     * @param instance workflow instance
     * @param node current task node
     * @param initialInput workflow initial input
     * @return resolved input publisher
     */
    Mono<Object> resolveInput(
            WorkflowInstance instance,
            TaskNode node,
            Object initialInput
    );
}