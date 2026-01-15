# FlowForge

**Forge reactive workflows with precision.**

FlowForge is a lightweight, strongly-typed, reactive workflow orchestration engine for Spring Boot. It enables you to define, execute, and monitor complex business workflows composed of reusable tasks, executed asynchronously using Project Reactor.

FlowForge is designed for **in-process, short-lived workflows** where determinism, concurrency, and type safety matter more than heavyweight BPM or distributed durability.

---

## Why FlowForge?

Modern backend systems often require orchestrating multiple asynchronous operations with dependencies:

* Validations
* External service calls
* Parallel computations
* Conditional execution
* Retries and timeouts

FlowForge addresses this problem space with a **programmatic, reactive-first orchestration model**, without introducing:

* BPMN
* XML / JSON workflow definitions
* Blocking execution
* Heavy external infrastructure

---

## Core Characteristics

* **Reactive by design** – Built on Project Reactor (`Mono` / `Flux`)
* **Strongly typed tasks** – Compile-time safety for inputs and outputs
* **DAG-based execution** – Deterministic task ordering with parallelism
* **Asynchronous orchestration** – Non-blocking execution model
* **Pluggable and extensible** – Hooks, monitors, and schedulers
* **Spring Boot native** – Auto-detection via annotations and components

---

## Non-Goals (Explicit Scope)

FlowForge is intentionally **not**:

* A distributed workflow engine
* A durable or persistent workflow system
* A BPMN or visual modeling tool
* A replacement for Temporal, Camunda, or Conductor

If your use case requires long-running workflows, crash recovery, or human-in-the-loop processes, FlowForge is not the right tool.

---

## Architecture Overview

FlowForge follows a programmatic orchestration model composed of the following core components:

```
Workflow Definition
        ↓
Workflow Registry
        ↓
Workflow Execution Plan (DAG)
        ↓
Reactive Workflow Orchestrator
        ↓
Reactive Execution Context
```

### Key Components

#### Workflow Orchestrator

Responsible for:

* Executing workflows
* Scheduling tasks respecting dependencies
* Handling retries and timeouts
* Managing concurrency

#### Executio
