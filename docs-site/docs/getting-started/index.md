---
title: "Getting Started"
sidebar_label: "Getting Started"
---




# Getting Started

This guide will walk you through setting up FlowForge and creating your first type-safe workflow.

## 1. Installation

FlowForge requires **Java 17+** and is designed to be used as a library within your application.

### Gradle
```gradle
dependencies {
    implementation("org.royada.flowforge:flowforge-spring-boot-starter:1.1.0")
}
```

### Maven
```xml
<dependency>
  <groupId>org.royada.flowforge</groupId>
  <artifactId>flowforge-spring-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```



## 2. Define Task and Flow

Create your tasks and orchestration using annotations. FlowForge will handle the registration during Spring's startup scanning.

```java
@Component
@TaskHandler
public class MyTasks {
    @FlowTask(id = "calculate")
    public Mono<Integer> calculate(Integer input) {
        return Mono.just(input * 2);
    }
}

@Component
@FlowWorkflow(id = "order-process")
public class OrderProcessWorkflow implements WorkflowDefinition {
    @Override
    public WorkflowExecutionPlan define(FlowDsl dsl) {
        // High-level orchestration for the E-Commerce pipeline
        return dsl.flow(MyTasks::calculate)
                  .build();
    }
}
```


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
        return client.executeResult("order-process", null)
                     .cast(Integer.class);
    }
}
```

You can also execute with a client-side timeout:

```java
client.execute("order-process", null, Duration.ofMillis(500));
```

Input contract:

- If your root task input type is non-`Void`, provide a compatible `input` in `execute/executeResult`.
- If your roots are `Void`, extra input is ignored (legacy-friendly behavior).


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
