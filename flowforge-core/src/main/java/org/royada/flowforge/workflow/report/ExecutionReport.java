package org.royada.flowforge.workflow.report;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.instance.TaskStatus;

/**
 * Summary report of a workflow execution.
 * Contains final status per task, durations, error causes, and aggregate
 * counters.
 */
public final class ExecutionReport {

  private final Map<TaskId, TaskStatus> finalStatuses;
  private final Map<TaskId, Duration> durations;
  private final Map<TaskId, Throwable> errors;
  private final int totalTasks;
  private final int completedTasks;
  private final int failedTasks;
  private final int skippedTasks;
  private final int maxInFlight;

  /**
   * Creates an immutable execution report.
   *
   * @param finalStatuses final status by task id
   * @param durations duration by task id
   * @param errors failure by task id
   * @param totalTasks total task count
   * @param completedTasks completed task count
   * @param failedTasks failed task count
   * @param skippedTasks skipped task count
   * @param maxInFlight maximum concurrent tasks observed
   */
  public ExecutionReport(
      Map<TaskId, TaskStatus> finalStatuses,
      Map<TaskId, Duration> durations,
      Map<TaskId, Throwable> errors,
      int totalTasks,
      int completedTasks,
      int failedTasks,
      int skippedTasks,
      int maxInFlight) {
    this.finalStatuses = Map.copyOf(finalStatuses);
    this.durations = Map.copyOf(durations);
    this.errors = Map.copyOf(errors);
    this.totalTasks = totalTasks;
    this.completedTasks = completedTasks;
    this.failedTasks = failedTasks;
    this.skippedTasks = skippedTasks;
    this.maxInFlight = maxInFlight;
  }

  /**
   * Returns the final status of each task in the workflow.
   * 
   * @return final status map
   */
  public Map<TaskId, TaskStatus> getFinalStatuses() {
    return finalStatuses;
  }

  /**
   * Returns the recorded duration for all tasks.
   * 
   * @return task durations map
   */
  public Map<TaskId, Duration> getDurations() {
    return durations;
  }

  /**
   * Returns all recorded errors from task executions.
   * 
   * @return task errors map
   */
  public Map<TaskId, Throwable> getErrors() {
    return errors;
  }

  /**
   * Returns the duration for a specific task.
   * 
   * @param taskId task id
   * @return duration for task if available
   */
  public Optional<Duration> getDuration(TaskId taskId) {
    return Optional.ofNullable(durations.get(taskId));
  }

  /**
   * Returns the error for a specific task, if it failed.
   * 
   * @param taskId task id
   * @return error for task if available
   */
  public Optional<Throwable> getError(TaskId taskId) {
    return Optional.ofNullable(errors.get(taskId));
  }

  /**
   * Returns the total number of tasks in the workflow.
   * 
   * @return total tasks in workflow
   */
  public int getTotalTasks() {
    return totalTasks;
  }

  /**
   * Returns the count of successfully completed tasks.
   * 
   * @return completed tasks count
   */
  public int getCompletedTasks() {
    return completedTasks;
  }

  /**
   * Returns the count of failed tasks.
   * 
   * @return failed tasks count
   */
  public int getFailedTasks() {
    return failedTasks;
  }

  /**
   * Returns the count of skipped tasks.
   * 
   * @return skipped tasks count
   */
  public int getSkippedTasks() {
    return skippedTasks;
  }

  /**
   * Returns the maximum number of tasks executing concurrently.
   * 
   * @return maximum tasks in flight observed
   */
  public int getMaxInFlight() {
    return maxInFlight;
  }

  @Override
  public String toString() {
    return "ExecutionReport{" +
        "total=" + totalTasks +
        ", completed=" + completedTasks +
        ", failed=" + failedTasks +
        ", skipped=" + skippedTasks +
        ", maxInFlight=" + maxInFlight +
        '}';
  }
}
