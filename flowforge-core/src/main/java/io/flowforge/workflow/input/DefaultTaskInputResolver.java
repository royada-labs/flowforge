package io.flowforge.workflow.input;

import io.flowforge.task.TaskId;
import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.instance.WorkflowInstance;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultTaskInputResolver implements TaskInputResolver {

    @Override
    public Mono<Object> resolveInput(
            WorkflowInstance instance,
            TaskNode node,
            Object initialInput
    ) {
        // 1) Roots → input inicial
        if (node.isRoot()) {
            return Mono.justOrEmpty(initialInput);
        }

        // 2) Una dependencia → output directo
        if (node.dependencies().size() == 1) {
            TaskNode dep = node.dependencies().iterator().next();
            return Mono.justOrEmpty(
                    instance.context().get(dep.id(), Object.class).orElse(null)
            );
        }

        // 3) Varias dependencias → map id -> output
        Map<TaskId, Object> inputs = new LinkedHashMap<>();
        for (TaskNode dep : node.dependencies()) {
            Object value =
                    instance.context().get(dep.id(), Object.class).orElse(null);
            inputs.put(dep.id(), value);
        }
        return Mono.just(inputs);
    }
}
