package org.royada.flowforge.task;

/**
 * Represents the result of a task execution.
 */
public sealed interface TaskResult
        permits TaskResult.Success,
        TaskResult.Failure,
        TaskResult.Skipped {

    /**
     * Returns whether the task execution was successful.
     * 
     * @return {@code true} if successful, {@code false} otherwise
     */
    boolean isSuccess();

    /**
     * Returns whether the task execution failed.
     * 
     * @return {@code true} if failed, {@code false} otherwise
     */
    boolean isFailure();

    /**
     * Returns whether the task execution was skipped.
     * 
     * @return {@code true} if skipped, {@code false} otherwise
     */
    boolean isSkipped();

    /**
     * Represents a successful task execution result.
     * 
     * @param output the task output
     */
    public record Success(Object output) implements TaskResult {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public boolean isSkipped() {
            return false;
        }
    }

    /**
     * Represents a failed task execution result.
     * 
     * @param error the error that caused the failure
     */
    public record Failure(Throwable error) implements TaskResult {

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public boolean isSkipped() {
            return false;
        }
    }

    /**
     * Represents a skipped task execution result.
     */
    public record Skipped() implements TaskResult {

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public boolean isSkipped() {
            return true;
        }
    }
}