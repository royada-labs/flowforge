package io.tugrandsolutions.flowforge.workflow.input;

import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.instance.WorkflowInstance;
import reactor.core.publisher.Mono;

public interface TaskInputResolver {

    Mono<Object> resolveInput(
            WorkflowInstance instance,
            TaskNode node,
            Object initialInput
    );
}