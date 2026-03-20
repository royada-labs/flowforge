package org.royada.flowforge.exception;

/**
 * Thrown when a requested workflow ID is not found.
 */
public class UnknownWorkflowException extends FlowForgeException {
  /**
   * Creates a new exception with the given workflow ID.
   * 
   * @param workflowId the missing workflow ID
   */
  public UnknownWorkflowException(String workflowId) {
    super("Unknown workflow ID: " + workflowId);
  }
}
