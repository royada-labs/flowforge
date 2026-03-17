package io.flowforge.task;

public sealed interface TaskResult
        permits TaskResult.Success,
        TaskResult.Failure,
        TaskResult.Skipped {

    boolean isSuccess();

    boolean isFailure();

    boolean isSkipped();

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