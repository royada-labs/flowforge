package io.flowforge.workflow.policy;

import reactor.core.publisher.Mono;

public interface ExecutionPolicy {

    <T> Mono<T> apply(Mono<T> taskExecution);

    default ExecutionPolicy andThen(ExecutionPolicy next) {
        ExecutionPolicy current = this;
        ExecutionPolicy following = java.util.Objects.requireNonNull(next, "next");
        return new ExecutionPolicy() {
            @Override
            public <T> Mono<T> apply(Mono<T> taskExecution) {
                return following.apply(current.apply(taskExecution));
            }
        };
    }

    static ExecutionPolicy defaultPolicy() {
        return new ExecutionPolicy() {
            @Override
            public <T> Mono<T> apply(Mono<T> taskExecution) {
                return taskExecution;
            }
        };
    }
}
