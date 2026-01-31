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
*   **Fail-Fast**: Validates workflows (cycles, usage) at startup.
*   **Type-Safe**: Generic task handlers.
*   **Observability**: Detailed execution reports and monitoring hooks.

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
    *   `Mono<ReactiveExecutionContext> execute(String workflowId, Object input, Duration timeout)`: Executes with a global timeout.
    *   `Mono<Object> executeResult(String workflowId, Object input)`: Executes and extracts the final result (for simple single-output flows).
*   **`WorkflowMonitor`**: Interface for listening to workflow lifecycle events (start, complete, task start/fail/success).
*   **`ExecutionReport`**: A summary object containing final statuses, durations, and errors for all tasks, generated upon workflow completion.

### 3.3. **Execution Policies**

*   **`RetryPolicy`**: Configurable retry logic (fixed, backoff) for transient failures.
*   **`TimeoutPolicy`**: Enforces a maximum duration for individual task execution.
*   *(Note: Policies are currently modeled in Core `TaskDescriptor`. Spring DSL support for attaching policies is internal or forthcoming.)*

### 3.4. Architecture

*   **Directed Acyclic Graph (DAG)**: Workflows are modeled as DAGs. The engine prevents cycles.
*   **Orchestration**: Uses a `ReactiveWorkflowOrchestrator` which implements a **Single-Writer** model:
    *   **State Scheduler**: A dedicated single-threaded scheduler manages all state mutations (transitions, queueing) to ensure concurrency safety without locks.
    *   **Task Scheduler**: Tasks execution involves IO/Computation and runs on `Schedulers.boundedElastic()` to avoid blocking the state loop.
*   **Observability**: The orchestrator tracks metrics (durations, in-flight count) and generates an `ExecutionReport` at the end of every run.

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

### 4.3. Executing a Workflow (Full Context)

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

### 4.4. Executing a Workflow (Result Only)

```java
public Mono<String> processData(String input) {
    // For workflows with a single "terminal" output, getting the result directly is cleaner
    return flowForge.executeResult("dataProcessingFlow", input)
           .cast(String.class);
}
```

### 4.5. Core / Standalone Usage (No Spring)

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

This context is sufficient to understand the library's scope and implementation details.
