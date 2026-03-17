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

### Define a Task
Create a Spring Component that implements `FlowTaskHandler`.

```java
@FlowTask(id = "calculateLength")
@Component
public class LengthTask implements FlowTaskHandler<Void, Integer> {
    @Override
    public Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
        return Mono.just(5);
    }
}
```

### Compose the Workflow
Use the `FlowDsl` to wire tasks together.

```java
@Configuration
public class MyFlowConfig {
    
    // Type-safe handles
    public static final TaskDefinition<Void, Integer> LENGTH = 
        TaskDefinition.of("calculateLength", Void.class, Integer.class);

    @FlowWorkflow(id = "first-flow")
    @Bean
    public WorkflowExecutionPlan myPlan(FlowDsl dsl) {
        return dsl.startTyped(LENGTH)
                  .build();
    }
}
```

### Compose Without Manual `TaskDefinition`

You can also define workflows with typed method references to `@FlowTask` bean methods.

```java
@Configuration
public class MyFlowConfig {

    @Bean
    @FlowTask(id = "producer")
    FlowTaskHandler<Void, Integer> producer() { ... }

    @Bean
    @FlowTask(id = "toStringTask")
    FlowTaskHandler<Integer, String> toStringTask() { ... }

    @FlowWorkflow(id = "method-ref-flow")
    @Bean
    WorkflowExecutionPlan myPlan(FlowDsl dsl) {
        return dsl.flow(MyFlowConfig::producer)
                  .then(MyFlowConfig::toStringTask)
                  .build();
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

You can group task methods in a Spring bean annotated with `@TaskHandler` and annotate each method with `@FlowTask`.

```java
@TaskHandler
class CustomerTasks {
    @FlowTask(id = "producer")
    Mono<Integer> producer(Void in, ReactiveExecutionContext ctx) { ... }

    @FlowTask(id = "formatter")
    Mono<String> formatter(Integer in, ReactiveExecutionContext ctx) { ... }
}

@Bean
WorkflowExecutionPlan plan(FlowDsl dsl) {
    return dsl.flow(CustomerTasks::producer)
              .then(CustomerTasks::formatter)
              .build();
}
```
