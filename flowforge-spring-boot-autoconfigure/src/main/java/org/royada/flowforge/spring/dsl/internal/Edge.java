package org.royada.flowforge.spring.dsl.internal;

import org.royada.flowforge.task.TaskId;

/**
 * Directed edge between two task ids in the DSL graph.
 *
 * @param from source task id
 * @param to target task id
 */
public record Edge(TaskId from, TaskId to) {
    /**
     * Validates edge endpoints are non-null.
     */
    public Edge {
        if (from == null) throw new IllegalArgumentException("from");
        if (to == null) throw new IllegalArgumentException("to");
    }
}
