package io.tugrandsolutions.flowforge.task;

import lombok.Value;

import java.util.Objects;

@Value
public class TaskId {
    String value;

    public static TaskId of(String value) {
        return new TaskId(Objects.requireNonNull(value));
    }
}