# FlowForge

**Forge reactive workflows with precision.**

FlowForge is a lightweight, strongly-typed, reactive workflow orchestration engine for Java. It enables you to define, execute, and monitor complex business workflows composed of reusable tasks, executed asynchronously using Project Reactor.

FlowForge is designed for **embedded, high-concurrency, short-lived workflows** where determinism, type safety, and observability matter more than heavyweight BPM or distributed durability.

---

## 🚀 Key Features

### ✨ Reactive & Non-Blocking
Built entirely on **Project Reactor**. Tasks are executed as `Mono<T>` pipelines, maximizing resource utilization via non-blocking I/O.

### 🛡️ Production-Ready Reliability
- **Dead-End Detection**: Automatically detects invalid workflow states where no progress is possible (runtime deadlocks) and fails fast with `DeadEndException`.
- **Cycle Detection**: Validates the DAG structure at build time to prevent infinite loops.
- **Strict Contracts**: Handles edge cases like `Mono.empty()` or synchronous exceptions by normalizing them into explicit failures, preventing silent hangs.

### ⏱️ Control & Lifecycle
- **Cooperative Cancellation**: Instantly stops workflow planning and cancels running reactive streams when requested.
- **Double Timeout Strategy**:
    - **Global Timeout**: Enforce a hard limit on the entire workflow execution.
    - **Per-Task Timeout**: smart handling where `Optional` tasks verify timeout without failing the workflow, while `Required` tasks enforce strict SLAs.

### 🔍 Deep Observability
- **Execution Reports**: Generates detailed `ExecutionReport` containing final task statuses, individual durations, error stack traces, and concurrency metrics.
- **Race-Free Monitoring**: Guarantees that the final report is available to monitors *before* the workflow emits its final signal.
- **Ordered Events**: Deterministic event stream (`TaskStart` -> `TaskSuccess`/`Failure`) for external monitoring systems.

---

## 📦 Usage Examples

### 1. Defining Tasks

```java
TaskId FETCH_DATA = new TaskId("fetch-data");
TaskId PROCESS_DATA = new TaskId("process-data");

Task<String, String> fetchTask = new BasicTask<>(FETCH_DATA) {
    @Override
    protected Mono<String> doExecute(String input, ReactiveExecutionContext context) {
        return webClient.get().uri("/data").retrieve().bodyToMono(String.class);
    }
};

Task<String, Result> processTask = new BasicTask<>(PROCESS_DATA) {
    @Override
    public Set<TaskId> dependencies() {
        return Set.of(FETCH_DATA);
    }

    @Override
    protected Mono<Result> doExecute(String input, ReactiveExecutionContext context) {
        // Input is automatically resolved from dependency output
        return service.process(input); 
    }
};
```

### 2. Building and Validating the Plan

```java
// Automatic validation of cycles, duplicates, and missing dependencies
WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(fetchTask, processTask));
```

### 3. Executing with Policies

```java
ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

// Execute with a global timeout of 5 seconds
orchestrator.execute(plan, "initial-input", Duration.ofSeconds(5))
    .doOnNext(context -> {
        Result result = context.get(PROCESS_DATA, Result.class).orElseThrow();
        System.out.println("Workflow finished: " + result);
    })
    .doOnError(TimeoutException.class, e -> System.err.println("Workflow timed out!"))
    .doOnError(DeadEndException.class, e -> System.err.println("Workflow stuck in dead-end!"))
    .subscribe();
```

### 4. Monitoring

```java
WorkflowMonitor monitor = new WorkflowMonitor() {
    @Override
    public void onWorkflowComplete(WorkflowInstance instance, ExecutionReport report) {
        System.out.println("Total duration: " + report.getTotalDuration());
        System.out.println("Failed tasks: " + report.getFailedTasks());
        
        if (report.getFinalStatuses().get(FETCH_DATA) == TaskStatus.FAILED) {
            log.error("Fetch failed: ", report.getError(FETCH_DATA).orElse(null));
        }
    }
};

// Orchestrator with custom monitor
new ReactiveWorkflowOrchestrator(
    Schedulers.parallel(), 
    monitor, 
    new DefaultTaskInputResolver()
);
```

---

## 🏗️ Architecture

FlowForge follows a programmatic orchestration model:

1.  **Workflow Definition**: Strongly typed `Task` objects defining logic and dependencies.
2.  **Validation**: Structural analysis of the DAG (Directed Acyclic Graph).
3.  **Orchestrator**: Event-driven engine using extensive `Sinks` and `Schedulers`.
    - **State Loop**: Single-threaded serializer for state updates (lock-free safety).
    - **Worker Loop**: Parallel execution of tasks on bounded schedulers.
4.  **Execution Context**: Thread-safe storage for task outputs passing data downstream.

---

## 🚫 Scope (Non-Goals)

FlowForge is intentionally **not**:
* A distributed workflow engine (no database persistence required).
* A BPMN visual tool.
* A replacement for Temporal/Camunda (use those for long-running, durable, human-centric processes).

**Use FlowForge for**: High-frequency composite API handling, parallel data aggregation, and complex in-memory business logic pipelines.
