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

  public Map<TaskId, TaskStatus> getFinalStatuses() {
    return finalStatuses;
  }

  public Map<TaskId, Duration> getDurations() {
    return durations;
  }

  public Map<TaskId, Throwable> getErrors() {
    return errors;
  }

  public Optional<Duration> getDuration(TaskId taskId) {
    return Optional.ofNullable(durations.get(taskId));
  }

  public Optional<Throwable> getError(TaskId taskId) {
    return Optional.ofNullable(errors.get(taskId));
  }

  public int getTotalTasks() {
    return totalTasks;
  }

  public int getCompletedTasks() {
    return completedTasks;
  }

  public int getFailedTasks() {
    return failedTasks;
  }

  public int getSkippedTasks() {
    return skippedTasks;
  }

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
