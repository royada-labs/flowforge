# Project Structure

This section provides a **recommended** package structure for applications using FlowForge. While the library is flexible and adapts to any architecture, following these patterns helps maintain a clear separation between business orchestration and task execution.

## The Core Pattern: Workflows & Tasks

For most Spring Boot applications, a binary separation between the "blueprint" and the "logic" is the most effective approach.

```text
com.yourcompany.project
├── 📁 workflows              # Business Orchestration (The "What")
│   ├── UserOnboardingFlow.java
│   └── OrderProcessFlow.java
│
└── 📁 tasks                  # Concrete Implementations (The "How")
    ├── SaveUserTask.java      # Annotated with @TaskHandler
    ├── NotifyUserTask.java
    └── ValidationsTask.java
```

### 1. The `workflows` Package
This package contains your **Flow Definitions**. 

*   **Role**: Here you use the `FlowDsl` to define the graph, branches, and error handling of your process.
*   **Best Practice**: Keep these classes as "clean" as possible. They should be readable by a non-technical stakeholder to understand the business logic. Avoid direct I/O or heavy computations here.

### 2. The `tasks` Package
This package contains your **Task Handlers**.

*   **Role**: Every class here is a reusable component annotated with `@TaskHandler`. This is where the actual work happens (Database queries, API calls, Calculations).
*   **Best Practice**: Make your tasks atomic. A good task does one thing well and can be reused in different workflows if needed.

---

## Data Management (FlowKeys)

One of FlowForge's biggest advantages for Spring Boot users (the **Annotation Way**) is that **defining explicit FlowKeys is often optional**.

*   **Automatic Inversion**: FlowForge can automatically derive input and output keys from your `@TaskHandler` method signatures.
*   **When to use manual FlowKeys?**: We suggest centralizing `FlowKey` definitions only for **Shared Global Context** or cross-cutting data (like `CorrelationId`, `TenantId`, or common Metadata) that many tasks need to access outside of the direct input/output chain.

### Recommendation for Key Management:
*   **Small Projects**: Rely on automatic annotation inference.
*   **Large/Enterprise Projects**: Use a central `context` or `io` package for shared metadata keys to maintain a single source of truth for global workflow state.

## Summary of Benefits
*   **Maintainability**: If the business process changes, you only modify the `workflows` package. If the technology changes (e.g., switching from REST to gRPC), you only modify the `tasks` package.
*   **Testability**: You can unit test a single `TaskHandler` independently of the entire workflow.
*   **Readability**: New developers can understand the entire system by simply browsing the `workflows` package.

> [!TIP]
> This structure is a suggestion based on industry best practices for orchestration engines. Feel free to adapt it to your specific architectural needs (Hexagonal, DDD, etc.).
