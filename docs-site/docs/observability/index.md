---
title: "Observability Guide"
sidebar_label: "Observability"
---



## 2. OpenTelemetry Integration

FlowForge has native support for **OpenTelemetry (OTel)**. When enabled, your workflows contribute to distributed traces.

### Configuration (Spring Boot)

```yaml
flowforge:
  tracing:
    opentelemetry:
      enabled: true
```

### What gets captured?

1.  **Workflow Span**: A parent span representing the total execution time.
2.  **Task Spans**: Child spans for each task.
3.  **Dependency Links**: When a task depends on another, FlowForge adds a **Span Link**. This allows visualization tools (like Jaeger or Honeycomb) to reconstruct the DAG accurately, even with parallel branches.

### Span Attributes

Each span is enriched with library-specific attributes:
- `flowforge.workflow.id`
- `flowforge.task.id`
- `flowforge.execution.id`
- `flowforge.task.input.type`
- `flowforge.task.output.type`
- `flowforge.task.status` (SUCCESS, ERROR, SKIPPED)
