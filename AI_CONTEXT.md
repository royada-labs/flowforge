# FlowForge AI Context

This document provides comprehensive context for AI agents working with the FlowForge library. It covers the library's purpose, architecture, core concepts, and usage patterns.

## 1. Overview

**FlowForge** is a lightweight, strongly-typed, reactive workflow orchestration engine for Java. It is built on top of **Project Reactor** (`Mono`, `Flux`) and is designed for:
*   **Embedded Use**: Runs within the application (no external orchestrator).
*   **High Concurrency**: Non-blocking, event-driven architecture.
*   **Short-Lived Workflows**: Ideal for API orchestration, data processing pipelines, etc.
*   **Spring Boot Integration**: First-class support with auto-configuration.

**Key Characteristics:**
*   **Reactive**: Fully non-blocking.
*   **Declarative DSL**: Fluent API for defining workflows.
*   **Fail-Fast**: Validates workflows (cycles, usage) at startup.
*   **Type-Safe**: Generic task handlers.

## 2. Project Structure

The project is a multi-module Gradle project:

*   **`flowforge-core`**: The core workflow engine, independent of Spring. Contains the orchestration logic, execution context, and plan definitions.
*   **`flowforge-spring-boot-autoconfigure`**: Spring Boot integration. Contains the auto-configuration, annotations (`@FlowTask`, `@FlowWorkflow`), and Spring-specific implementations.
*   **`flowforge-spring-boot-starter`**: Starter module pulling in the necessary dependencies.

## 3. Core Concepts & API

### 3.1. Annotations (Spring Support)

*   **`@FlowTask(id = "...")`**: Marks a Spring Bean as a workflow task. The bean must implement `FlowTaskHandler`.
    *   `id`: Unique identifier for the task within the application.
*   **`@FlowWorkflow(id = "...")`**: Marks a `@Bean` method as a workflow definition. The bean must return `WorkflowExecutionPlan`.
    *   `id`: Unique identifier for the workflow.

### 3.2. Interfaces

*   **`FlowTaskHandler<I, O>`**: Interface for implementing task logic.
    *   `Mono<O> execute(I input, ReactiveExecutionContext ctx)`: The core execution method.
*   **`FlowDsl`**: Entry point for defining workflows.
    *   `FlowBuilder start(String taskId)`: Starts a workflow definition.
*   **`FlowBuilder`**: Fluent API for building the workflow graph.
    *   `then(String taskId)`: Sequential execution.
    *   `fork(Consumer<FlowBranch>... branches)`: Parallel execution.
    *   `join(String taskId)`: Merge parallel branches.
    *   `build()`: Finalizes the workflow and returns `WorkflowExecutionPlan`.
*   **`ReactiveExecutionContext`**: Context passed to tasks.
    *   `put(TaskId id, T value)`: Store intermediate results.
    *   `get(TaskId id, Class<T> type)`: Retrieve results from other tasks.
*   **`FlowForgeClient`**: The primary interface for triggering workflows.
    *   `Mono<ReactiveExecutionContext> execute(String workflowId, Object input)`: Executes a workflow.

### 3.3. Architecture

*   **Directed Acyclic Graph (DAG)**: Workflows are modeled as DAGs. The engine prevents cycles.
*   **Orchestration**: Uses a `ReactiveWorkflowOrchestrator` which manages the state changes and task scheduling using Reactor's schedulers.
*   **Task Execution**: Tasks are executed on a bounded elastic scheduler to avoid blocking the main event loop.

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

### 4.2. Defining a Workflow

```java
@Configuration
public class UserOnboardingWorkflow {

    @FlowWorkflow(id = "onboarding")
    @Bean
    public WorkflowExecutionPlan onboardingFlow(FlowDsl dsl) {
        return dsl.start("fetchUserData")
                  .then("validateUser")
                  .fork(
                      b -> b.then("sendWelcomeEmail"),
                      b -> b.then("provisionResources")
                  )
                  .join("finalizeOnboarding")
                  .build();
    }
}
```

### 4.3. Executing a Workflow

```java
@Service
public class OnboardingService {
    private final FlowForgeClient flowForge;

    public OnboardingService(FlowForgeClient flowForge) {
        this.flowForge = flowForge;
    }

    public Mono<Void> startOnboarding(String userId) {
        return flowForge.execute("onboarding", userId).then();
    }
}
```

## 5. Development Guidelines

*   **Immutability**: Prefer immutable data structures for task inputs/outputs.
*   **Non-Blocking**: Ensure `execute` methods are non-blocking. Wrap blocking calls in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
*   **Error Handling**: Exceptions in tasks propagate up and fail the workflow execution unless handled within the task.
*   **Testing**: Use `StepVerifier` from `reactor-test` to test workflows and clients.

## 6. Common Issues & Solutions

*   **Cycle Detection**: If the application fails to start with a cycle error, check your DSL definition.
*   **Missing Task**: Ensure all task IDs referenced in the DSL correspond to beans annotated with `@FlowTask`.

This context is sufficient to understand the library's scope and implementation details.
