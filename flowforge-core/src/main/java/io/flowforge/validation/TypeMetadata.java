package io.flowforge.validation;

import java.util.Objects;

/**
 * Describes type information associated with a task definition.
 *
 * <p>Instances are collected during typed DSL usage and used by the
 * {@link FlowValidator} to perform type compatibility checks across edges.
 *
 * @param inputType  the expected input type of the task
 * @param outputType the expected output type of the task
 */
public record TypeMetadata(Class<?> inputType, Class<?> outputType) {

    /**
     * Creates a validated {@code TypeMetadata}.
     *
     * @throws NullPointerException if either type is null
     */
    public TypeMetadata {
        Objects.requireNonNull(inputType, "inputType");
        Objects.requireNonNull(outputType, "outputType");
    }
}
