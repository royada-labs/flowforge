# FlowForge

**Forge reactive workflows with precision.**

FlowForge is a lightweight, strongly-typed, reactive workflow orchestration engine for Java. It enables you to define, validate, execute, and observe complex business workflows composed of reusable tasks, executed asynchronously using Project Reactor.

Unlike traditional workflow engines, FlowForge focuses on **embedded, high-concurrency, short-lived workflows** where type safety and observability are paramount.

---

## 🚀 Quick Start (Typed DSL)

```java
public class UserOnboarding {
    // 1. Define your tasks with types
    static TaskDefinition<Void, User> FETCH = TaskDefinition.of("fetch", Void.class, User.class);
    static TaskDefinition<User, Profile> ENRICH = TaskDefinition.of("enrich", User.class, Profile.class);

    @Bean
    public WorkflowExecutionPlan onboardingFlow(FlowDsl dsl) {
        // 2. Compose with compile-time type safety
        return dsl.startTyped(FETCH)
                  .then(ENRICH)
                  .build();
    }

}
```

---

## ✨ Key Features

*   🛡️ **Bulletproof Type-Safety**: No runtime casting. Type propagation is enforced by the compiler across the DAG.
*   ⚡ **Reactive Execution**: Built on **Project Reactor**. Fully non-blocking and backpressure-ready.
*   🧱 **Fail-Fast Validation**: Advanced DAG analysis detects cycles and missing dependencies at startup.
*   🔍 **Deep Observability**: Native integration with **OpenTelemetry** and structured JSON execution tracing.
*   📦 **Spring Boot Optimized**: Zero-config starter with auto-discovery of tasks and workflows.

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

---

## 📄 License

Licensed under the [Apache License 2.0](LICENSE).
Copyright 2026 Rolando Rodríguez González.
