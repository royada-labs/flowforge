# FlowForge

**Forge reactive workflows with precision.**

FlowForge is a lightweight, strongly-typed, reactive workflow orchestration engine for Java. It enables you to define, validate, execute, and observe complex business workflows composed of reusable tasks, executed asynchronously using Project Reactor.

Unlike traditional workflow engines, FlowForge focuses on **embedded, high-concurrency, short-lived workflows** where type safety and observability are paramount.

---

## 🚀 Quick Start (Annotation-First)

```java
@TaskHandler("customer")
class CustomerTasks {
  @FlowTask(id = "getUser")
  Mono<User> getUser(Void in) { ... } // no context needed

  @FlowTask(id = "getOrders")
  Mono<OrderSummary> getOrders(User in) { ... } // getUser output -> getOrders input

  @FlowTask(id = "discount")
  Mono<Discount> discount(OrderSummary in, ReactiveExecutionContext ctx) { ... }
}

@Bean
WorkflowExecutionPlan plan(FlowDsl dsl) {
    return dsl.flow(CustomerTasks::getUser)
              .then(CustomerTasks::getOrders)
              .then(CustomerTasks::discount)
              .build();
}
```

Alternative: explicit `TaskDefinition<I,O>` is still available for advanced/centralized contracts.

```java
TaskDefinition<Void, User> GET_USER = TaskDefinition.of("getUser", Void.class, User.class);
TaskDefinition<User, OrderSummary> GET_ORDERS = TaskDefinition.of("getOrders", User.class, OrderSummary.class);
TaskDefinition<OrderSummary, Discount> DISCOUNT = TaskDefinition.of("discount", OrderSummary.class, Discount.class);

@Bean
WorkflowExecutionPlan plan(FlowDsl dsl) {
  return dsl.startTyped(GET_USER)
            .then(GET_ORDERS)
            .then(DISCOUNT)
            .build();
}
```

`ReactiveExecutionContext` is optional in `@FlowTask` methods. Inject it only when the task needs to read/write additional values from context; otherwise keep the signature minimal (`input -> output`).

In sequential composition (`flow(...).then(...)`), FlowForge automatically feeds the previous task output as the next task input.

---

## ✨ Key Features

*   🛡️ **Bulletproof Type-Safety**: No runtime casting. Type propagation is enforced by the compiler across the DAG.
*   ⚡ **Reactive Execution**: Built on **Project Reactor**. Fully non-blocking and backpressure-ready.
*   🧱 **Fail-Fast Validation**: Advanced DAG analysis detects cycles and missing dependencies at startup.
*   🔍 **Deep Observability**: Native integration with **OpenTelemetry** and structured JSON execution tracing.
*   📦 **Spring Boot Optimized**: Zero-config starter with auto-discovery of tasks and workflows.
*   🔐 **Safe Task Registration**: Startup-time detection rejects duplicate/conflicting task metadata and ambiguous method-reference mappings.

---

## 🔒 Registration & Resolution Guarantees

FlowForge resolves method references using the **full JVM signature** (`implClass + method + descriptor`), not by heuristic name fallback.

- Duplicate `TaskDefinition` identities are rejected at startup.
- Conflicting method-reference registrations are rejected at startup.
- Overloaded task methods are resolved deterministically by signature.
- Reflection/introspection happens at startup only; runtime executes pre-built task plans.

### Root Input Contract

- If root tasks require non-`Void` input, callers must provide `client.execute(..., input)`.
- If all roots are `Void`, extra input is ignored for backward compatibility.
### Client Timeouts

You can also execute workflows with a client-side timeout:

```java
client.execute("my-workflow", input, Duration.ofMillis(500))
```

This will cancel the execution and fail if the workflow does not complete in the given duration.
---

## 📦 Installation

### Gradle
```gradle
implementation("io.flowforge:flowforge-spring-boot-starter:1.0.0")
```

### Maven
```xml
<dependency>
  <groupId>io.flowforge</groupId>
  <artifactId>flowforge-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

---

## 📚 Documentation

Detailed guides and references are available in the `/docs` directory:

1.  [**Getting Started**](docs/getting-started/index.md) - Installation and your first workflow.
2.  [**Core Concepts**](docs/core-concepts/index.md) - Architecture, types, and the execution model.
3.  [**Examples**](docs/examples/index.md) - Real-world scenarios (API orchestration, data pipelines).
4.  [**Observability**](docs/observability/index.md) - Tracing and OpenTelemetry metrics.
5.  [**API Reference**](docs/api-reference/index.md) - Deep dive into core interfaces.
6.  [**Migration Prompt Guide**](docs/migration-prompt-guide.md) - Prompt/checklist to migrate legacy implementations safely.

---

## 📄 License

Licensed under the [Apache License 2.0](LICENSE).
Copyright 2026 Rolando Rodríguez González.
