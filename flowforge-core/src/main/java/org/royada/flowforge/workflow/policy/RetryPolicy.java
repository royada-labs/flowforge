package org.royada.flowforge.workflow.policy;

import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * {@link ExecutionPolicy} that re-subscribes to a task when it terminates with an error.
 *
 * <p><strong>When it runs:</strong> this policy is applied when the orchestrator executes a task
 * through {@link org.royada.flowforge.task.TaskDescriptor#policy()}.
 *
 * <p><strong>What triggers retries:</strong> retries are triggered by terminal errors emitted by
 * the task {@link Mono} (that is, {@code onError} signals). In this class, retry filtering is not
 * customized, so behavior is delegated to Reactor's {@link Retry} defaults.
 *
 * <p><strong>Backoff strategies:</strong>
 * <ul>
 *   <li>{@link #fixed(int)} retries immediately up to {@code maxRetries}.</li>
 *   <li>{@link #backoff(int, Duration)} retries with exponential backoff starting at
 *       {@code minBackoff}.</li>
 * </ul>
 *
 * <p><strong>Interaction with timeouts:</strong> retry/timeout interaction depends on policy
 * composition order. If timeout is applied per attempt before retry, timeout errors can be retried.
 * If timeout wraps the whole retried sequence, a single outer timeout can fail the task regardless
 * of remaining retries.
 */
public final class RetryPolicy implements ExecutionPolicy {

  private final Retry retrySpec;

  private RetryPolicy(Retry retrySpec) {
    this.retrySpec = retrySpec;
  }

  /**
   * Creates a retry policy with no backoff delay between attempts.
   *
   * @param maxRetries maximum number of retry attempts after the initial attempt
   * @return a fixed-delay retry policy
   */
  public static RetryPolicy fixed(int maxRetries) {
    return new RetryPolicy(Retry.max(maxRetries));
  }

  /**
   * Creates a retry policy with exponential backoff.
   *
   * @param maxRetries maximum number of retry attempts after the initial attempt
   * @param minBackoff minimum backoff duration for the first retry
   * @return an exponential backoff retry policy
   */
  public static RetryPolicy backoff(int maxRetries, Duration minBackoff) {
    return new RetryPolicy(Retry.backoff(maxRetries, minBackoff));
  }

  @Override
  public <T> Mono<T> apply(Mono<T> taskExecution) {
    return taskExecution.retryWhen(retrySpec);
  }
}
