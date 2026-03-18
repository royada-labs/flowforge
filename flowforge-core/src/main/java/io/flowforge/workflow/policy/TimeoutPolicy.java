package io.flowforge.workflow.policy;

import java.time.Duration;

import reactor.core.publisher.Mono;

public final class TimeoutPolicy implements ExecutionPolicy {

  private final Duration timeout;

  private TimeoutPolicy(Duration timeout) {
    this.timeout = timeout;
  }

  public static TimeoutPolicy of(Duration timeout) {
    return new TimeoutPolicy(timeout);
  }

  @Override
  public <T> Mono<T> apply(Mono<T> taskExecution) {
    return taskExecution.timeout(timeout);
  }
}
