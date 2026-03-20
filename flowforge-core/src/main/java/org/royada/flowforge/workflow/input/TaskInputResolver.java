package org.royada.flowforge.workflow.input;

import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
import reactor.core.publisher.Mono;

public interface TaskInputResolver {

    Mono<Object> resolveInput(
            WorkflowInstance instance,
            TaskNode node,
            Object initialInput
    );
}