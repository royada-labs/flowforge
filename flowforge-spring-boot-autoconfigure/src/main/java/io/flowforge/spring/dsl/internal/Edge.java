package io.flowforge.spring.dsl.internal;

public record Edge(String from, String to) {
    public Edge {
        if (from == null || from.isBlank()) throw new IllegalArgumentException("from");
        if (to == null || to.isBlank()) throw new IllegalArgumentException("to");
    }
}