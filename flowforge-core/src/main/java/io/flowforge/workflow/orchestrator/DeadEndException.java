package io.flowforge.workflow.orchestrator;

import java.util.Set;

import io.flowforge.task.TaskId;

/**
 * Thrown when the workflow reaches a dead-end state at runtime:
 * - No tasks are ready to run
 * - No tasks are currently running
 * - Workflow is not finished
 * 
 * This indicates an inconsistent state that should not occur with proper
 * validation.
 */
public final class DeadEndException extends RuntimeException {

  private final Set<TaskId> pendingTasks;

  public DeadEndException(String message, Set<TaskId> pendingTasks) {
    super(message);
    this.pendingTasks = pendingTasks;
  }

  public Set<TaskId> getPendingTasks() {
    return pendingTasks;
  }
}
