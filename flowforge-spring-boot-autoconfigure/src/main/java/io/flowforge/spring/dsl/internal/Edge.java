package io.flowforge.spring.dsl.internal;

import io.flowforge.task.TaskId;

public record Edge(TaskId from, TaskId to) {
    public Edge {
        if (from == null) throw new IllegalArgumentException("from");
        if (to == null) throw new IllegalArgumentException("to");
    }
}
