# Observability

FlowForge is designed to give you total visibility into how your workflows execute.

## 1. Execution Trace

When debugging, you often want to see exactly what happened step-by-step. FlowForge provides a detailed `ExecutionTrace`.

```java
client.executeWithTrace("my-flow", input)
      .subscribe(trace -> {
          System.out.println("Trace ID: " + trace.traceId());

          trace.tasks().forEach(task -> {
              System.out.printf("[%s] %s - %dms\n",
                  task.status(), task.taskId(), task.durationMs());
          });
      });
```

The trace includes:
- Start and end timestamps for the workflow and every task.
- Success, Error, or Skipped status for each task.
- Detailed error messages and stack traces if a task fails.

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

---

## 3. Visualizing the workflow

FlowForge can export your workflow definition to **Mermaid** format, which can be rendered directly in GitHub or VS Code.

```java
FlowVisualization viz = FlowVisualizer.visualize(plan, FlowValidationResult.of(List.of()));
String mermaid = viz.toMermaid();
```

This ensures that your documentation and your code stay in sync perfectly.
