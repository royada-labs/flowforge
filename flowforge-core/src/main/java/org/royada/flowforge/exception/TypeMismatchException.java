package org.royada.flowforge.exception;

import org.royada.flowforge.task.TaskId;

/**
 * Thrown when there is a mismatch between the expected and actual type of a task result.
 */
public class TypeMismatchException extends FlowForgeException {
    /** The ID of the task with the type mismatch. */
    private final TaskId taskId;
    /** The expected output type. */
    private final Class<?> expectedType;
    /** The actual output type found. */
    private final Class<?> actualType;

    /**
     * Creates a new exception with the given task ID and types.
     * 
     * @param taskId the ID of the task
     * @param expectedType the expected output type
     * @param actualType the actual output type found
     */
    public TypeMismatchException(TaskId taskId, Class<?> expectedType, Class<?> actualType) {
        super(String.format("Type mismatch for task '%s': expected %s but found %s",
                taskId.getValue(), expectedType.getSimpleName(), actualType.getSimpleName()));
        this.taskId = taskId;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    /**
     * Returns the task ID.
     * 
     * @return the task ID
     */
    public TaskId getTaskId() {
        return taskId;
    }

    /**
     * Returns the expected type.
     * 
     * @return the expected type
     */
    public Class<?> getExpectedType() {
        return expectedType;
    }

    /**
     * Returns the actual type.
     * 
     * @return the actual type
     */
    public Class<?> getActualType() {
        return actualType;
    }
}
