package org.royada.flowforge.spring.registry;

import org.royada.flowforge.api.FlowTaskHandler;
import org.royada.flowforge.task.TaskId;

/**
 * Immutable view of a registered {@link FlowTaskHandler} and its metadata.
 *
 * @param id task identifier
 * @param optional whether task failures are optional/skippable
 * @param handler handler instance
 */
public record RegisteredTaskHandler(
        TaskId id,
        boolean optional,
        FlowTaskHandler<?, ?> handler
) {}