package org.royada.flowforge.exception;

import java.util.Set;

import org.royada.flowforge.task.TaskId;

/**
 * Thrown when the workflow reaches a dead-end state at runtime:
 * - No tasks are ready to run
 * - No tasks are currently running
 * - Workflow is not finished
 * 
 * This indicates an inconsistent state that should not occur with proper
 * validation.
 */
public class DeadEndException extends FlowForgeException {

  /** The set of tasks that were still pending when the dead-end was detected. */
  private final Set<TaskId> pendingTasks;

  /**
   * Creates a new dead-end exception.
   * 
   * @param message exception message
   * @param pendingTasks tasks still pending when dead-end was detected
   */
  public DeadEndException(String message, Set<TaskId> pendingTasks) {
    super(message);
    this.pendingTasks = pendingTasks;
  }

  /**
   * Returns the set of pending tasks at the time the dead-end was detected.
   * 
   * @return pending tasks at dead-end detection time
   */
  public Set<TaskId> getPendingTasks() {
    return pendingTasks;
  }
}
