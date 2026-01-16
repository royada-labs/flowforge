package io.tugrandsolutions.flowforge.workflow.policy;

import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public final class RetryPolicy implements ExecutionPolicy {

  private final Retry retrySpec;

  private RetryPolicy(Retry retrySpec) {
    this.retrySpec = retrySpec;
  }

  public static RetryPolicy fixed(int maxRetries) {
    return new RetryPolicy(Retry.max(maxRetries));
  }

  public static RetryPolicy backoff(int maxRetries, Duration minBackoff) {
    return new RetryPolicy(Retry.backoff(maxRetries, minBackoff));
  }

  @Override
  public <T> Mono<T> apply(Mono<T> taskExecution) {
    return taskExecution.retryWhen(retrySpec);
  }
}
