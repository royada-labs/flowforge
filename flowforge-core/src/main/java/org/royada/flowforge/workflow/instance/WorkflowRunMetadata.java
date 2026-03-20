package org.royada.flowforge.workflow.instance;

import java.util.Map;

/**
 * Metadata for a workflow run, including ID, correlation information, and tags.
 * 
 * @param workflowId the unique identifier for the workflow
 * @param correlationId an optional correlation ID for tracing
 * @param tags optional metadata tags
 */
public record WorkflowRunMetadata(
    String workflowId,
    String correlationId,
    Map<String, String> tags) {
  
  /**
   * Creates a new metadata instance with validation.
   * 
   * @param workflowId the unique identifier for the workflow
   * @param correlationId an optional correlation ID for tracing
   * @param tags optional metadata tags
   */
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

  /**
   * Creates a metadata instance with only the workflow ID.
   * 
   * @param workflowId the workflow ID
   * @return the metadata instance
   */
  public static WorkflowRunMetadata of(String workflowId) {
    return new WorkflowRunMetadata(workflowId, null, Map.of());
  }

  /**
   * Returns a new instance with the given correlation ID.
   * 
   * @param correlationId the correlation ID to set
   * @return the new metadata instance
   */
  public WorkflowRunMetadata withCorrelationId(String correlationId) {
    return new WorkflowRunMetadata(workflowId, correlationId, tags);
  }

  /**
   * Returns a new instance with the given tags.
   * 
   * @param tags the tags to set
   * @return the new metadata instance
   */
  public WorkflowRunMetadata withTags(Map<String, String> tags) {
    return new WorkflowRunMetadata(workflowId, correlationId, tags);
  }
}
