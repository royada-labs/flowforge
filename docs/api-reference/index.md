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

---

## 2. `@TaskHandler` + `@FlowTask` (Primary Declaration Model)

Recommended way to declare tasks:

```java
@TaskHandler("customer")
class CustomerTasks {
    @FlowTask(id = "getUser")
    Mono<User> getUser(Void in) { ... }
}
```

`ReactiveExecutionContext` is optional for `@FlowTask` methods. Use it only when the task needs contextual reads/writes.

`@FlowTask` also supports optional metadata for advanced workflows:

- `optional = true`: marks a task as skippable if its dependencies fail.
- `dependsOn = {"taskA", "taskB"}`: declares extra dependency edges beyond the sequential/DSL flow.
- `inputType = X.class` / `outputType = Y.class`: manually override inferred input/output types (useful for complex generics or when the compiler cannot infer the types reliably).

#### FAQ: ¿Cuándo usar cada opción?

- **`optional=true`**: usa esto cuando la tarea no sea crítica y deba poder saltarse si una dependencia falla (p.ej., notificaciones secundarias).
- **`dependsOn`**: útil cuando necesitas expresar dependencias adicionales que no aparecen en el flujo principal del DSL (ej. un task de limpieza que siempre debe ejecutarse después de un grupo de tareas).
- **`inputType/outputType`**: útil cuando el compilador no puede inferir correctamente el tipo (por ejemplo, `Mono<?>` retornado o cuando se usan wrappers genéricos). Evita `ClassCastException` en tiempo de ejecución.

You can still use `FlowTaskHandler` bean methods and `TaskDefinition` where needed.

---

## 3. `TaskDefinition<I, O>`

Immutable metadata describing a task.

- **`of(id, inputClass, outputClass)`**: Static factory method.
- **`outputKey()`**: Returns a `FlowKey<O>` used to retrieve data from the context.

---

## 4. `ReactiveExecutionContext`

The state container for an active execution. It is available to tasks that explicitly declare it as a parameter.

- **`get(FlowKey<T> key)`**: Returns `Optional<T>`.
- **`getOrThrow(FlowKey<T> key)`**: Returns `T` or throws `NoSuchElementException`.
- **`getOrDefault(key, default)`**: Returns value or fallback.

---

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

---

## 6. `FlowDsl`

The fluent builder for workflows.

- **`startTyped(taskDef)`**: Begins a typed flow and enables type propagation.
- **`start(methodRef)`**: Starts from a typed method reference to an `@FlowTask` bean method.
- **`flow(methodRef)`**: Alias of `start(methodRef)` for ultra-fluent style.
- **`then(taskDef)`**: Adds a sequential typed dependency.
- **`then(methodRef)`**: Adds a sequential typed dependency using method references.
- **`fork(builders...)`**: Creates parallel branches.
- **`parallel(methodRef...)`**: Convenience varargs for parallel branches from the current tail.
- **`join(taskDef)`**: Synchronizes parallel branches into a single typed task.
- **`join(methodRef)`**: Synchronizes branches using method references.

In sequential chains, FlowForge automatically wires `previousOutput -> nextInput`.

---

## 7. `ExecutionTracer`

An SPI for capturing workflow events.

- **`OpenTelemetryExecutionTracer`**: Implementation that sends data to OTel.
- **`DefaultExecutionTracer`**: Implementation that builds the in-memory `ExecutionTrace`.

---

## 8. Type Safety Guarantees

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

Method reference guarantees:

- `@FlowTask` metadata (task id + input/output types) is inferred and stored at startup.
- Method references are resolved by full signature (`implClass + method + descriptor`) when building the workflow definition.
- Runtime execution uses the compiled plan directly (no reflection in task execution path).
- Name-based fallback resolution is intentionally disabled to avoid accidental task mapping.

Root input guarantees:

- Roots with non-`Void` input require caller-provided initial input at `execute(...)`.
- Roots with `Void` input ignore extra initial input for backward compatibility.
