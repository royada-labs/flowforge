# Observability: Logs & Tracing

FlowForge is designed to be fully observable. In a reactive environment, where tasks jump between threads, knowing exactly what happened and when is critical.

---

## 🔍 Log Monitoring

By default, FlowForge includes an **`AsyncLoggingWorkflowMonitor`**. This is what generates the structured logs you see in your console:

```text
2026-03-21... INFO --- Workflow started: workflowId=order-process instance=973219190
2026-03-21... INFO --- Task started: taskId=fetchOrder
2026-03-21... INFO --- Task finished: taskId=fetchOrder outcome=SUCCESS
```

### Enable/Disable Logging
You can control the default logging behavior via `application.yml`:

```yaml
flowforge:
  monitoring:
    logging:
      enabled: true # Set to false to hide automatic task logs
```

### Custom Monitors
If you want to send metrics to Prometheus, Datadog, or your own database, you can implement the `WorkflowMonitor` interface and register it as a Spring Bean:

```java
@Component
public class MetricsMonitor implements WorkflowMonitor {
    @Override
    public void onTaskSuccess(WorkflowInstance instance, TaskId taskId, Duration duration) {
        // 📊 Publish metric to Micrometer/Prometheus
        Metrics.timer("workflow.task.duration", "taskId", taskId.getValue())
               .record(duration);
    }
}
```

---

## 🧶 Execution Tracing (Gantt-style)

Beyond simple logs, FlowForge can capture a full **Execution Trace**. This trace contains the exact start/stop timestamps for every task and their dependencies.

### Getting a Trace
Instead of using `client.execute()`, use `executeWithTrace()`:

```java
public Mono<Void> debugOrder(Order order) {
    return client.executeWithTrace("order-process", order)
            .doOnNext(trace -> {
                // 🔍 Iterate over every task execution
                trace.getTasks().forEach(task -> {
                    System.out.println(task.getTaskId() + " took " + task.getDurationMillis() + "ms");
                });
            })
            .then();
}
```

### Why use Traces?
1.  **Bottleneck Detection**: Find which parallel branch is slowing down the whole workflow.
2.  **Failure Analysis**: See exactly where a sequence broke and what the state of other parallel tasks was at that moment.
3.  **Audit Logs**: Store the trace in a database for regulatory compliance of every step taken.
