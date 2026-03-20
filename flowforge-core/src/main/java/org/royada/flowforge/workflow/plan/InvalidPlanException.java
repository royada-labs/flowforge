package org.royada.flowforge.workflow.plan;

/**
 * Thrown when a workflow plan is invalid and cannot be executed.
 * This includes issues like:
 * - Duplicate task IDs
 * - Missing dependencies
 * - Cycles in the dependency graph
 * - Invalid root configuration
 */
public final class InvalidPlanException extends RuntimeException {

  /**
   * Creates a new exception with the given message.
   * 
   * @param message the error message
   */
  public InvalidPlanException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the given message and cause.
   * 
   * @param message the error message
   * @param cause the cause of the failure
   */
  public InvalidPlanException(String message, Throwable cause) {
    super(message, cause);
  }
}
