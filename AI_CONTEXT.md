# FlowForge AI Context

This document provides comprehensive context for AI agents working with the FlowForge library. It covers the library's purpose, architecture, core concepts, and usage patterns.

## 1. Overview

**FlowForge** is a lightweight, strongly-typed, reactive workflow orchestration engine for Java. It is built on top of **Project Reactor** (`Mono`, `Flux`) and is designed for:
*   **Embedded Use**: Runs within the application (no external orchestrator).
*   **High Concurrency**: Non-blocking, event-driven architecture using a single-writer pattern for state safety.
*   **Short-Lived Workflows**: Ideal for API orchestration, data processing pipelines, etc.
*   **Spring Boot Integration**: First-class support with auto-configuration.
*   **Resilience**: Built-in support for **Timeouts** and **Retries** via Execution Policies.

**Key Characteristics:**
*   **Reactive**: Fully non-blocking.
*   **Declarative DSL**: Fluent API for defining workflows.
*   **Bulletproof Type-Safety**: End-to-end type safety from DSL definition to context retrieval using `TaskDefinition` and `FlowKey`.
*   **Observability-first**: Deep integration with **OpenTelemetry** with span linking for DAG dependencies.
*   **Defensive Runtime**: Explicit type validation at the context level and custom exception hierarchy.

## 2. Project Structure

The project is a multi-module Gradle project:

*   **`flowforge-core`**: The core workflow engine, independent of Spring. Contains the orchestration logic, execution context, plan definitions, and policies.
*   **`flowforge-spring-boot-autoconfigure`**: Spring Boot integration. Contains the auto-configuration, annotations (`@FlowTask`, `@FlowWorkflow`), and Spring-specific implementations.
*   **`flowforge-spring-boot-starter`**: Starter module pulling in the necessary dependencies.
*   **`flowforge-stress-harness`**: A testing module for validating high-concurrency and load scenarios.

## 3. Core Concepts & API

### 3.1. Annotations (Spring Support)

*   **`@FlowTask(id = "...")`**: Marks a Spring Bean as a workflow task. The bean must implement `FlowTaskHandler`.
    *   `id`: Unique identifier for the task within the application.
*   **`@FlowWorkflow(id = "...")`**: Marks a `@Bean` method as a workflow definition. The bean must return `WorkflowExecutionPlan`.
    *   `id`: Unique identifier for the workflow.

### 3.2. Interfaces & Classes

*   **`TaskDefinition<I, O>`**: Metadata for a task, defining its ID, input type, and output type. It is the core of the type-safe API.
*   **`FlowKey<T>`**: A type-safe handle for retrieving task results. Created via `TaskDefinition.outputKey()`.
*   **`TypedFlowBuilder<O>`**: An evolved builder that tracks the output type of the current tail, enabling chaining via `.then(TaskDefinition)`.
*   **`ReactiveExecutionContext`**: Context passed to tasks.
    *   `get(FlowKey<T> key)`: Retrieve results with runtime type safety.
    *   `getOrThrow(FlowKey<T> key)`: Retrieve or throw `NoSuchElementException` / `TypeMismatchException`.
    *   `getOrDefault(FlowKey<T> key, T defaultValue)`: Retrieve with a fallback value.
*   **`WorkflowMonitor` / `ExecutionTracer`**: Interfaces for observability. `OpenTelemetryExecutionTracer` provides distributed tracing with dependency links.
*   **`ExecutionReport`**: A summary object containing final statuses, durations, and errors.

### 3.3. **Execution Policies**

*   **`RetryPolicy`**: Configurable retry logic (fixed, backoff) for transient failures.
*   **`TimeoutPolicy`**: Enforces a maximum duration for individual task execution.
*   *(Note: Policies are currently modeled in Core `TaskDescriptor`. Spring DSL support for attaching policies is internal or forthcoming.)*

### 3.4. Exception Hierarchy

*   **`FlowForgeException`**: Base class for all library exceptions.
*   **`TypeMismatchException`**: Thrown when a context value exists but does not match the expected type in `FlowKey`.
*   **`WorkflowExecutionException`**: Thrown when a workflow fails execution.

### 3.5. Architecture

*   **Directed Acyclic Graph (DAG)**: Workflows are modeled as DAGs.
*   **Orchestration**: Uses a `ReactiveWorkflowOrchestrator` with a **Single-Writer** model for state safety.
*   **Observability**: Integrated with OpenTelemetry via `ExecutionTracer`. Spans for tasks include links to their dependent spans for clear DAG visualization in tools like Jaeger.

## 4. Usage Examples

### 4.1. Defining a Task

```java
@FlowTask(id = "fetchUserData")
@Component
public class FetchUserDataTask implements FlowTaskHandler<String, UserProfile> {
    @Override
    public Mono<UserProfile> execute(String userId, ReactiveExecutionContext ctx) {
        return userRepository.findById(userId);
    }
}
```

### 4.2. Defining a Workflow (Typed DSL)

```java
@Configuration
public class UserOnboardingWorkflow {

    public static TaskDefinition<String, UserProfile> FETCH = TaskDefinition.of("fetchUserData", String.class, UserProfile.class);
    public static TaskDefinition<UserProfile, UserProfile> VALIDATE = TaskDefinition.of("validateUser", UserProfile.class, UserProfile.class);

    @FlowWorkflow(id = "onboarding")
    @Bean
    public WorkflowExecutionPlan onboardingFlow(FlowDsl dsl) {
        return dsl.startTyped(FETCH)
                  .then(VALIDATE)
                  .fork(
                      b -> b.then("sendWelcomeEmail"),
                      b -> b.then("provisionResources")
                  )
                  .build();
    }
}
```

### 4.3. Safe Data Access in Tasks

```java
@FlowTask(id = "validateUser")
public class ValidateTask implements FlowTaskHandler<UserProfile, UserProfile> {
    @Override
    public Mono<UserProfile> execute(UserProfile user, ReactiveExecutionContext ctx) {
        // Access data from unreachable or previous tasks using FlowKey
        FlowKey<Status> statusKey = TaskDefinition.of("getStatus", Void.class, Status.class).outputKey();
        Status s = ctx.getOrDefault(statusKey, Status.PENDING);
        
        return Mono.just(user);
    }
}
```

### 4.4. Executing a Workflow (Full Context)

```java
@Service
public class OnboardingService {
    private final FlowForgeClient flowForge;

    public OnboardingService(FlowForgeClient flowForge) {
        this.flowForge = flowForge;
    }

    public Mono<Void> startOnboarding(String userId) {
        return flowForge.execute("onboarding", userId)
            .doOnNext(ctx -> {
                 // Access intermediate results if needed
            })
            .then();
    }
}
```

### 4.5. Executing a Workflow (Result Only)

```java
public Mono<String> processData(String input) {
    // For workflows with a single "terminal" output, getting the result directly is cleaner
    return flowForge.executeResult("dataProcessingFlow", input)
           .cast(String.class);
}
```

### 4.6. Core / Standalone Usage (No Spring)

FlowForge can be used without Spring by manually creating tasks and the orchestrator.

```java
// 1. Define Tasks
class MyTask extends BasicTask<String, Integer> {
    public MyTask() { super(new TaskId("myTask")); }
    
    @Override
    protected Mono<Integer> doExecute(String input, ReactiveExecutionContext ctx) {
        return Mono.just(input.length());
    }
}

// 2. Build Plan
List<Task<?, ?>> tasks = List.of(new MyTask());
WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);

// 3. Create Orchestrator
ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

// 4. Execute
orchestrator.execute("myWorkflow", plan, "Hello World")
    .subscribe(ctx -> {
        System.out.println("Workflow Finished");
    });
```

## 5. Development Guidelines

*   **Immutability**: Prefer immutable data structures for task inputs/outputs.
*   **Non-Blocking**: Ensure `execute` methods are non-blocking. Tasks run on the elastic pool, but blocking the thread is still discouraged.
*   **Error Handling**: Exceptions in tasks propagate up and fail the workflow execution unless handled within the task or by a Policy.
*   **Testing**: Use `StepVerifier` from `reactor-test` to test workflows and clients.

## 6. Common Issues & Solutions

*   **Cycle Detection**: If the application fails to start with a cycle error, check your DSL definition.
*   **Missing Task**: Ensure all task IDs referenced in the DSL correspond to beans annotated with `@FlowTask`.
*   **Timeout**: If a workflow hangs, ensure you are using the `timeout` overload or that your tasks have internal timeouts.

## 7. Licensing

FlowForge is licensed under the **Apache License 2.0**.
*   **Copyright 2026 Rolando Rodríguez González**
*   All artifacts (JARs) include `LICENSE` and `NOTICE` files in `META-INF`.
*   Public API classes include a standard license header.
*   The `build.gradle` file is configured to include license metadata in Maven POMs.

This context is sufficient to understand the library's scope, implementation details, and compliance requirements.

