# API Reference

A technical overview of the primary interfaces and classes in FlowForge.

## 1. `FlowForgeClient`

The primary entry point for interacting with workflows.

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `execute(workflowId, input)` | `Mono<ReactiveExecutionContext>` | Triggers execution and returns the final context. |
| `executeResult(workflowId, input)` | `Mono<Object>` | Convenience method to get the final task's output. |
| `executeWithTrace(workflowId, input)` | `Mono<ExecutionTrace>` | Executes and returns a detailed execution trace (JSON/pretty print). |

---

## 2. `TaskDefinition<I, O>`

Immutable metadata describing a task.

- **`of(id, inputClass, outputClass)`**: Static factory method.
- **`outputKey()`**: Returns a `FlowKey<O>` used to retrieve data from the context.

---

## 3. `ReactiveExecutionContext`

The state container for an active execution. Each task receives an instance in its `execute` method.

- **`get(FlowKey<T> key)`**: Returns `Optional<T>`.
- **`getOrThrow(FlowKey<T> key)`**: Returns `T` or throws `NoSuchElementException`.
- **`getOrDefault(key, default)`**: Returns value or fallback.

---

## 4. `FlowTaskHandler<I, O>`

The interface your tasks must implement.

```java
public interface FlowTaskHandler<I, O> {
    Mono<O> execute(I input, ReactiveExecutionContext ctx);
}
```

---

## 5. `FlowDsl`

The fluent builder for workflows.

- **`startTyped(taskDef)`**: Begins a typed flow and enables type propagation.
- **`then(taskDef)`**: Adds a sequential typed dependency.
- **`fork(builders...)`**: Creates parallel branches.
- **`join(taskDef)`**: Synchronizes parallel branches into a single typed task.

---

## 6. `ExecutionTracer`

An SPI for capturing workflow events.

- **`OpenTelemetryExecutionTracer`**: Implementation that sends data to OTel.
- **`DefaultExecutionTracer`**: Implementation that builds the in-memory `ExecutionTrace`.

---

## 7. Type Safety Guarantees

FlowForge enforces typed chaining at compile time.

Valid chain:

```java
TaskDefinition<Void, Integer> A = TaskDefinition.of("A", Void.class, Integer.class);
TaskDefinition<Integer, String> B = TaskDefinition.of("B", Integer.class, String.class);
TaskDefinition<String, Long> C = TaskDefinition.of("C", String.class, Long.class);

dsl.startTyped(A)
   .then(B)
   .then(C)
   .build();
```

Invalid chain (does not compile):

```java
TaskDefinition<Void, Integer> A = TaskDefinition.of("A", Void.class, Integer.class);
TaskDefinition<Boolean, String> INCOMPATIBLE = TaskDefinition.of("X", Boolean.class, String.class);

dsl.startTyped(A)
   .then(INCOMPATIBLE); // compile-time type error
```
