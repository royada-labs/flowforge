package io.flowforge.exception;

public class UnknownWorkflowException extends RuntimeException {
  public UnknownWorkflowException(String workflowId) {
    super("Unknown workflow ID: " + workflowId);
  }
}
