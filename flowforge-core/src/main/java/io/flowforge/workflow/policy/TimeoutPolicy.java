package io.flowforge.workflow.policy;

import java.time.Duration;

import reactor.core.publisher.Mono;

/**
 * {@link ExecutionPolicy} that fails task execution if it does not complete within a duration.
 *
 * <p><strong>When it runs:</strong> this policy is applied when the orchestrator executes a task
 * through {@link io.flowforge.task.TaskDescriptor#policy()}.
 *
 * <p><strong>Behavior:</strong> delegates to {@link Mono#timeout(Duration)}. If the task does not
 * complete in time, the resulting publisher fails with a timeout error.
 *
 * <p><strong>Interaction with retries:</strong> timeout scope depends on composition order.
 * If timeout is applied to each attempt and then wrapped by retry, each timed-out attempt can be
 * retried. If retry is applied first and then wrapped by timeout, timeout acts as a global cap for
 * the whole retry sequence.
 */
public final class TimeoutPolicy implements ExecutionPolicy {

  private final Duration timeout;

  private TimeoutPolicy(Duration timeout) {
    this.timeout = timeout;
  }

  /**
   * Creates a timeout policy for a single task execution (or full wrapped sequence, depending on
   * policy composition order).
   *
   * @param timeout maximum allowed execution time
   * @return a timeout policy
   */
  public static TimeoutPolicy of(Duration timeout) {
    return new TimeoutPolicy(timeout);
  }

  @Override
  public <T> Mono<T> apply(Mono<T> taskExecution) {
    return taskExecution.timeout(timeout);
  }
}
