package org.royada.flowforge.exception;

public class UnknownWorkflowException extends FlowForgeException {
  public UnknownWorkflowException(String workflowId) {
    super("Unknown workflow ID: " + workflowId);
  }
}
