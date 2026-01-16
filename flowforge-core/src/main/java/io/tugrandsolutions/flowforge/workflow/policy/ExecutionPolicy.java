package io.tugrandsolutions.flowforge.workflow.policy;

import reactor.core.publisher.Mono;

public interface ExecutionPolicy {

    <T> Mono<T> apply(Mono<T> taskExecution);

    static ExecutionPolicy defaultPolicy() {
        return new ExecutionPolicy() {
            @Override
            public <T> Mono<T> apply(Mono<T> taskExecution) {
                return taskExecution;
            }
        };
    }
}
