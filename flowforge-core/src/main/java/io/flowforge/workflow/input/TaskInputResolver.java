package io.flowforge.workflow.input;

import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.instance.WorkflowInstance;
import reactor.core.publisher.Mono;

public interface TaskInputResolver {

    Mono<Object> resolveInput(
            WorkflowInstance instance,
            TaskNode node,
            Object initialInput
    );
}