package org.royada.flowforge.exception;

/**
 * Thrown when an error occurs during workflow execution.
 */
public class WorkflowExecutionException extends FlowForgeException {
  /** The workflow ID that failed execution. */
  private final String workflowId;

  /**
   * Creates a new exception with the given workflow ID and cause.
   * 
   * @param workflowId the workflow ID
   * @param cause the cause of the failure
   */
  public WorkflowExecutionException(String workflowId, Throwable cause) {
    super("Error executing workflow: " + workflowId, cause);
    this.workflowId = workflowId;
  }

  /**
   * Returns the workflow ID.
   * 
   * @return the workflow ID
   */
  public String getWorkflowId() {
    return workflowId;
  }
}
