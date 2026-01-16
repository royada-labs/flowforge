package io.tugrandsolutions.flowforge.workflow.plan;

/**
 * Thrown when a workflow plan is invalid and cannot be executed.
 * This includes issues like:
 * - Duplicate task IDs
 * - Missing dependencies
 * - Cycles in the dependency graph
 * - Invalid root configuration
 */
public final class InvalidPlanException extends RuntimeException {

  public InvalidPlanException(String message) {
    super(message);
  }

  public InvalidPlanException(String message, Throwable cause) {
    super(message, cause);
  }
}
