package io.tugrandsolutions.flowforge.workflow.instance;

import java.util.Map;

public record WorkflowRunMetadata(
    String workflowId,
    String correlationId,
    Map<String, String> tags) {
  public WorkflowRunMetadata {
    if (workflowId == null || workflowId.isBlank()) {
      throw new IllegalArgumentException("workflowId must not be null/blank");
    }
    if (correlationId != null && correlationId.isBlank()) {
      correlationId = null;
    }
    if (tags == null) {
      tags = Map.of();
    }
  }

  public static WorkflowRunMetadata of(String workflowId) {
    return new WorkflowRunMetadata(workflowId, null, Map.of());
  }

  public WorkflowRunMetadata withCorrelationId(String correlationId) {
    return new WorkflowRunMetadata(workflowId, correlationId, tags);
  }

  public WorkflowRunMetadata withTags(Map<String, String> tags) {
    return new WorkflowRunMetadata(workflowId, correlationId, tags);
  }
}
