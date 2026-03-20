package org.royada.flowforge.workflow.policy;

import reactor.core.publisher.Mono;

/**
 * Decorates task execution publishers with cross-cutting runtime policies
 * such as retry, timeout or custom behavior.
 */
public interface ExecutionPolicy {

    /**
     * Applies policy behavior to a task execution publisher.
     *
     * @param taskExecution original task execution publisher
     * @param <T> task output type
     * @return decorated publisher
     */
    <T> Mono<T> apply(Mono<T> taskExecution);

    /**
     * Composes this policy followed by {@code next}.
     *
     * @param next next policy to apply
     * @return composed policy
     */
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

    /**
     * @return no-op policy
     */
    static ExecutionPolicy defaultPolicy() {
        return new ExecutionPolicy() {
            @Override
            public <T> Mono<T> apply(Mono<T> taskExecution) {
                return taskExecution;
            }
        };
    }
}
