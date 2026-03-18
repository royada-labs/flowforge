# Getting Started

This guide will walk you through setting up FlowForge and creating your first type-safe workflow.

## 1. Installation

FlowForge requires **Java 17+** and is designed to be used as a library within your application.

### Gradle
```gradle
dependencies {
    implementation("io.flowforge:flowforge-spring-boot-starter:1.0.0")
}
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

## 2. Your First Workflow

A workflow consists of **Tasks** and a **Plan**.

### Recommended: Annotation-First (`@TaskHandler` + `@FlowTask`)

```java
@TaskHandler("demo")
class DemoTasks {
    @FlowTask(id = "calculateLength")
    Mono<Integer> calculateLength(Void input) {
        return Mono.just(5);
    }
}

@Configuration
class MyFlowConfig {
    @FlowWorkflow(id = "first-flow")
    @Bean
    WorkflowExecutionPlan myPlan(FlowDsl dsl) {
        return dsl.flow(DemoTasks::calculateLength).build();
    }
}
```

`ReactiveExecutionContext` is optional in `@FlowTask` methods. Add it only if that specific task needs context access.

### Alternative: Bean Methods Returning `FlowTaskHandler`

```java
@Configuration
public class MyFlowConfig {

    @Bean
    @FlowTask(id = "producer")
    FlowTaskHandler<Void, Integer> producer() { ... }

    @FlowWorkflow(id = "first-flow")
    @Bean
    WorkflowExecutionPlan myPlan(FlowDsl dsl) {
        return dsl.flow(MyFlowConfig::producer).build();
    }
}
```

### Advanced Alternative: Explicit `TaskDefinition<I,O>`

Use this when you prefer centralized static contracts.

```java
@Configuration
public class MyFlowConfig {
    static final TaskDefinition<Void, Integer> PRODUCER =
        TaskDefinition.of("producer", Void.class, Integer.class);

    @FlowWorkflow(id = "method-ref-flow")
    @Bean
    WorkflowExecutionPlan myPlan(FlowDsl dsl) {
        return dsl.startTyped(PRODUCER).build();
    }
}
```

---

## 3. Executing the Workflow

Inject `FlowForgeClient` and trigger the execution.

```java
@Service
public class MyService {
    private final FlowForgeClient client;

    public MyService(FlowForgeClient client) {
        this.client = client;
    }

    public Mono<Integer> process() {
        return client.executeResult("first-flow", null)
                     .cast(Integer.class);
    }
}
```

Input contract:

- If your root task input type is non-`Void`, provide a compatible `input` in `execute/executeResult`.
- If your roots are `Void`, extra input is ignored (legacy-friendly behavior).

---

## 4. Understanding Type Safety (FlowKey)

Traditional workflow engines often return maps of objects (`Map<String, Object>`), which lead to runtime `ClassCastException`.

FlowForge uses `FlowKey<T>`. When you define a task, you get a key that "remembers" the type.

```java
// Definition
TaskDefinition<Void, Integer> LENGTH_TASK = ...;

// Inside another task
FlowKey<Integer> key = LENGTH_TASK.outputKey();
Integer length = ctx.getOrThrow(key); // Automatically returns Integer
```

This guarantees that if the workflow compiles, the data types in your context are correct.

---

## 5. `@TaskHandler` Style (No Interface Boilerplate)

This is the primary recommended style in FlowForge.

```java
@TaskHandler
class CustomerTasks {
    @FlowTask(id = "producer")
    Mono<Integer> producer(Void in) { ... }

    @FlowTask(id = "formatter")
    Mono<String> formatter(Integer in) { ... }
}

@Bean
WorkflowExecutionPlan plan(FlowDsl dsl) {
    return dsl.flow(CustomerTasks::producer)
              .then(CustomerTasks::formatter)
              .build();
}
```

Sequential rule: the output type of each task is the input type of the next task in `.then(...)`. If the types do not match, it fails at compile time.
