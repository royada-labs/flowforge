package io.tugrandsolutions.flowforge.spring.exception;

public class WorkflowExecutionException extends RuntimeException {
  private final String workflowId;

  public WorkflowExecutionException(String workflowId, Throwable cause) {
    super("Error executing workflow: " + workflowId, cause);
    this.workflowId = workflowId;
  }

  public String getWorkflowId() {
    return workflowId;
  }
}
