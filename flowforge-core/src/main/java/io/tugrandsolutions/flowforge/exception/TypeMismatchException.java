package io.tugrandsolutions.flowforge.exception;

import io.tugrandsolutions.flowforge.task.TaskId;

/**
 * Thrown when there is a mismatch between the expected and actual type of a task result.
 */
public class TypeMismatchException extends FlowForgeException {
    private final TaskId taskId;
    private final Class<?> expectedType;
    private final Class<?> actualType;

    public TypeMismatchException(TaskId taskId, Class<?> expectedType, Class<?> actualType) {
        super(String.format("Type mismatch for task '%s': expected %s but found %s",
                taskId.getValue(), expectedType.getSimpleName(), actualType.getSimpleName()));
        this.taskId = taskId;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public Class<?> getExpectedType() {
        return expectedType;
    }

    public Class<?> getActualType() {
        return actualType;
    }
}
