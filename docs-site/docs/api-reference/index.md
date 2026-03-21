---
title: "API Reference"
sidebar_label: "API Reference"
---

# API Reference

A technical overview of the primary interfaces and classes in FlowForge.

## 1. `FlowForgeClient`

The primary entry point for interacting with workflows.

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `execute(workflowId, input)` | `Mono<ReactiveExecutionContext>` | Triggers execution and returns the final context. |
| `execute(workflowId, input, timeout)` | `Mono<ReactiveExecutionContext>` | Executes the workflow with a client-side timeout (errors if not complete in time). |
| `executeResult(workflowId, input)` | `Mono<Object>` | Convenience method to get the final task's output. |
| `executeWithTrace(workflowId, input)` | `Mono<ExecutionTrace>` | Executes and returns a detailed execution trace (JSON/pretty print). |


## 3. `TaskDefinition<I, O>`

Immutable metadata describing a task.

- **`of(id, inputClass, outputClass)`**: Static factory method.
- **`outputKey()`**: Returns a `FlowKey<O>` used to retrieve data from the context.


## 5. `FlowTaskHandler<I, O>`

Contract interface for functional bean-style tasks.

```java
public interface FlowTaskHandler<I, O> {
    Mono<O> execute(I input, ReactiveExecutionContext ctx);
}
```

`FlowTaskHandler` is optional. You can also use `@TaskHandler` classes with `@FlowTask` methods:

```java
@TaskHandler
class CustomerTasks {
    @FlowTask(id = "getUser")
    Mono<User> getUser(Void in, ReactiveExecutionContext ctx) { ... }
}
```


## 7. `ExecutionTracer`

An SPI for capturing workflow events.

- **`OpenTelemetryExecutionTracer`**: Implementation that sends data to OTel.
- **`DefaultExecutionTracer`**: Implementation that builds the in-memory `ExecutionTrace`.
