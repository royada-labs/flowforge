package io.flowforge.spring.registry;

import io.flowforge.api.FlowTaskHandler;
import io.flowforge.task.TaskId;

public record RegisteredTaskHandler(
        TaskId id,
        boolean optional,
        FlowTaskHandler<?, ?> handler
) {}