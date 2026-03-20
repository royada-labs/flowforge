package org.royada.flowforge.workflow.input;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
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
            if (node.descriptor().task().inputType() == Void.class) {
                return Mono.empty();
            }
            return Mono.justOrEmpty(initialInput);
        }

        // 2) Una dependencia → output directo
        if (node.dependencies().size() == 1) {
            TaskNode dep = node.dependencies().iterator().next();
            return Mono.justOrEmpty(
                    instance.context().get(dep.descriptor().task().outputKey()).orElse(null)
            );
        }

        // 3) Varias dependencias → map id -> output
        Map<TaskId, Object> inputs = new LinkedHashMap<>();
        for (TaskNode dep : node.dependencies()) {
            Object value =
                    instance.context().get(dep.descriptor().task().outputKey()).orElse(null);
            inputs.put(dep.id(), value);
        }
        return Mono.just(inputs);
    }
}
