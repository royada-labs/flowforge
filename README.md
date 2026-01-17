# FlowForge

**Forge reactive workflows with precision.**

FlowForge is a lightweight, strongly-typed, reactive workflow orchestration engine for Java. It enables you to define, execute, and monitor complex business workflows composed of reusable tasks, executed asynchronously using Project Reactor.

FlowForge is designed for **embedded, high-concurrency, short-lived workflows** where determinism, type safety, and observability matter more than heavyweight BPM or distributed durability.

---

## 🚀 Key Features

*   ✅ **Embedded**: No external runtime or database required.
*   ✅ **Reactive**: Fully built on Project Reactor (Mono/Flux), non-blocking by default.
*   ✅ **Declarative**: Define workflows using a fluent, type-safe DSL.
*   ✅ **Reusable**: Tasks are independent components that can participate in multiple workflows.
*   ✅ **Fail-Fast**: Invalid DAGs (cycles, broken dependencies) fail at startup.
*   ✅ **Spring-Native**: First-class Spring Boot integration with auto-configuration.

---

## 📦 Installation

Add the starter dependency to your project:

### Gradle

```gradle
implementation("io.tugrandsolutions.flowforge:flowforge-spring-boot-starter:0.1.0")
```

### Maven

```xml
<dependency>
  <groupId>io.tugrandsolutions.flowforge</groupId>
  <artifactId>flowforge-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

---

## 🛠️ Usage Guide

### 1. Defining a Task (`@FlowTask`)

A task is a standard Spring `@Component` that implements `FlowTaskHandler`. It is isolated, testable, and reusable.

```java
@FlowTask(id = "TaskA")
@Component
public class TaskA implements FlowTaskHandler<String, String> {

    @Override
    public Mono<String> execute(String input, ReactiveExecutionContext ctx) {
        return Mono.just(input + "-processed");
    }
}
```

### 2. Defining a Workflow (`@FlowWorkflow`)

Workflows are defined as `@Bean` methods that return a `WorkflowExecutionPlan`. The `FlowDsl` builder validates valid paths and broken dependencies.

```java
@Configuration
public class WorkflowConfig {

    @FlowWorkflow(id = "sampleFlow")
    @Bean
    WorkflowExecutionPlan sampleFlow(FlowDsl dsl) {
        return dsl
            .start("TaskA")
            .then("TaskB")
            .build();
    }
}
```

**Supported DSL Operations:**
*   `start(taskId)`: Defines the entry point.
*   `then(taskId)`: Chains tasks sequentially.
*   `fork(taskId...)`: Executes multiple tasks in parallel.
*   `join(taskId)`: Merges parallel branches (waits for dependencies).

Example of a complex flow:
```java
dsl.start("FetchData")
   .fork("ProcessImage", "AnalyzeText")
   .join("AggregateResults")
   .then("SaveToDb")
   .build();
```

### 3. Executing a Workflow

Inject `FlowForgeClient` and execute the workflow by its ID. It returns a `Mono<ReactiveExecutionContext>` containing the results.

```java
@Service
public class WorkflowService {

    private final FlowForgeClient client;

    public WorkflowService(FlowForgeClient client) {
        this.client = client;
    }

    public Mono<String> run() {
        return client.execute("sampleFlow", "initial-input")
                     .map(ctx -> ctx.get("TaskB", String.class).orElse("default"));
    }
}
```

---

## 🏗️ Architecture

FlowForge follows a programmatic orchestration model:

1.  **Workflow Definition**: Strongly typed objects defining logic and dependencies.
2.  **Validation**: Structural analysis of the DAG (Directed Acyclic Graph) at startup.
3.  **Orchestrator**: Event-driven engine using `Sinks` and `Schedulers`.
    - **State Loop**: Single-threaded serializer for state updates (lock-free safety).
    - **Worker Loop**: Parallel execution of tasks on bounded schedulers.
4.  **Execution Context**: Thread-safe storage for passing data downstream.

---

## 🚫 Scope (Non-Goals)

FlowForge is intentionally **not**:
*   A distributed workflow engine (no database persistence required).
*   A BPMN visual tool.
*   A replacement for Temporal/Camunda (use those for long-running, durable processes).

**Use FlowForge for**: High-frequency composite API handling, parallel data aggregation, and complex in-memory business logic pipelines.

