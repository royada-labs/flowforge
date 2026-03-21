# Configuration: Spring Boot Integration

FlowForge is easily configured using standard Spring Boot properties. This allows you to tune the reactive engine for high-load or specific monitoring requirements.

---

## ⚙️ Core Execution Properties

These properties control how the orchestrator manages its internal event queue and concurrency limits.

```yaml
flowforge:
  execution:
    # ⚡ Maximum number of tasks executing concurrently across all workflows.
    # Default: Number of CPU cores (min 2)
    max-in-flight-tasks: 8

    # 📥 Input queue size before applying backpressure.
    # Default: 1000
    max-queue-size: 500

    # 🛡️ Strategy when limits are exceeded: BLOCK, ERROR, or LATEST.
    # Default: BLOCK
    backpressure-strategy: BLOCK
```

---

## 🔍 Monitoring & Logs

Enable or disable the built-in observability features.

```yaml
flowforge:
  monitor:
    # 📡 Enables core monitor callbacks (required for logging and tracing).
    # Default: true
    enabled: true
```

---

## 🧶 Tracing (OpenTelemetry)

FlowForge provides native integration with OpenTelemetry to export traces to Zipkin, Jaeger, or any OTLP collector.

```yaml
flowforge:
  tracing:
    opentelemetry:
      # 🛰️ Enable automatic Span propagation for every task.
      # Default: false
      enabled: true
```

---

## 🧩 Advanced: Custom Schedulers

By default, the orchestrator uses **`Schedulers.boundedElastic()`** for task execution. If you need to run tasks on a specialized thread pool (e.g. for heavy CPU calculations), you can customize the `ReactiveWorkflowOrchestrator` bean manually in your configuration class.
