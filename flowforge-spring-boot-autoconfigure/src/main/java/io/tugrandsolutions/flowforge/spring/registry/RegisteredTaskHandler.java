package io.tugrandsolutions.flowforge.spring.registry;

import io.tugrandsolutions.flowforge.api.FlowTaskHandler;
import io.tugrandsolutions.flowforge.task.TaskId;

public record RegisteredTaskHandler(
        TaskId id,
        boolean optional,
        FlowTaskHandler<?, ?> handler
) {}