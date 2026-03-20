# Core Concepts

FlowForge is built on the philosophy that workflows should be as type-safe as the code that defines them.

## 1. Architecture

FlowForge operates in three distinct phases:

1.  **Definition (DSL)**: You define the graph using the `FlowDsl`. This phase is purely declarative.
2.  **Compilation (Plan)**: The DSL is transformed into a `WorkflowExecutionPlan`. During this phase, FlowForge validates the graph for cycles, missing tasks, and type mismatches.
3.  **Orchestration (Runtime)**: The `ReactiveWorkflowOrchestrator` consumes the Plan and executes tasks. It handles concurrency, state persistence (in-memory), and event propagation.

## 2. The Type System

At the heart of FlowForge are two primitives:

### Annotation-First Declaration
The recommended declaration style is `@TaskHandler` classes with `@FlowTask` methods, wired with typed method references in the DSL (`flow(...)`, `then(...)`, `fork(...)`, `join(...)`, `parallel(...)`).
`ReactiveExecutionContext` is optional in `@FlowTask` signatures and should be injected only when needed by that task.

### `TaskDefinition<I, O>`
Describes a task's identity and its "contract" (Input type `I` and Output type `O`). It acts as the blueprint for both the DSL and the data access.

### `FlowKey<T>`
A type-safe address to a value in the execution context. When a task with output type `T` completes, its result is stored under its `FlowKey<T>`. Downstream tasks use this key to retrieve the data without casting.

## 3. Execution Model (DAG + Reactor)

Workflows are modeled as a **Directed Acyclic Graph (DAG)**. 
In sequential edges (`then(...)`), task output is automatically used as input of the next task.

*   **Parallelism**: Tasks that do not depend on each other are executed concurrently.
*   **Reactive**: Every step is a `Mono`. There are no blocking threads waiting for tasks to finish.
*   **Single-Writer State**: State transitions in the orchestrator happen in a serialized manner (using a Reactor Sink), ensuring thread safety without heavy locks.

## 4. Error Handling

FlowForge provides several layers of protection:

*   **Validation Errors**: Caught at startup (e.g., `STARTUP_CYCLE_DETECTED`).
*   **Runtime Exceptions**: If a task throws an exception, the branch is marked as failed.
*   **Execution Policies**: You can attach `RetryPolicy` or `TimeoutPolicy` to tasks to handle transient failures or slow operations.
*   **Client Timeouts**: You can also apply a client-side timeout via `FlowForgeClient.execute(workflowId, input, timeout)` to cancel slow runs.

For practical diagnosis steps and remediation patterns, see the [Troubleshooting Guide](../troubleshooting.md).

## 5. Observability

FlowForge is "observability-first". 

*   **Internal Tracing**: Every execution produces an `ExecutionReport` and an optional `ExecutionTrace` (JSON).
*   **OpenTelemetry**: If enabled, every task becomes a Span in your distributed trace, complete with attributes for input/output types and links to dependent tasks.
