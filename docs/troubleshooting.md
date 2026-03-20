# Troubleshooting Guide

This guide covers common FlowForge issues and practical ways to diagnose and fix them.

## 1. Startup Errors

### `STARTUP_CYCLE_DETECTED`

**Symptom**: application startup fails with a cycle validation error.

**Why it happens**:
- The workflow graph contains a circular dependency.
- A task indirectly depends on itself through `.then(...)`, `.fork(...)`, `.join(...)`, or `dependsOn` metadata.

**How to fix**:
- Remove back-edges that point to upstream tasks.
- Keep flow definitions acyclic: every edge must move forward in the pipeline.
- Check `dependsOn` declarations in `@FlowTask` for accidental loops.

### Type mismatch in typed DSL chains

**Symptom**: `IllegalArgumentException` while defining a workflow, similar to:

```text
Type mismatch in workflow definition: task 'X' expects input type A but received output type B
```

**Why it happens**:
- The output type of the previous task is not assignable to the input type of the next task.

**How to fix**:
- Make task signatures compatible in `.then(...)` chains.
- Add a mapper task between incompatible types.
- If using custom `inputType/outputType` in `@FlowTask`, verify they match method signatures.

## 2. Runtime Failures

### Retry exhausted

**Symptom**: task still fails after retries.

**Why it happens**:
- The error is persistent, not transient.
- `maxRetries` is too low for your downstream stability profile.
- The failing operation is not idempotent and keeps failing under retry.

**How to debug**:
- Enable debug logging for `org.royada.flowforge`.
- Inspect task-level failure messages and final exception cause.
- Verify that the downstream dependency recovers within your retry window.

**Mitigations**:
- Increase retry attempts and/or use exponential backoff.
- Make the task idempotent before enabling aggressive retries.
- Add fallback behavior (for optional paths) when business rules allow it.

### Task timeout

**Symptom**: task fails with timeout before completion.

**Why it happens**:
- Task execution exceeds the configured timeout.
- Timeout value is lower than normal p95/p99 task latency.

**How to debug**:
- Compare observed task duration vs configured timeout.
- Check network/database latency during failures.
- Distinguish task-level timeout from client-side execute timeout.

**Mitigations**:
- Raise timeout to realistic SLOs.
- Optimize the task or split into smaller steps.
- Use retry only for transient timeout scenarios.

## 3. Workflow Debugging Checklist

- Enable `DEBUG` for `org.royada.flowforge` and task packages.
- Use `executeWithTrace(...)` to capture execution trace and task order.
- Verify task IDs are unique and method references resolve to expected handlers.
- Reproduce with a minimal workflow: start task + failing task + single dependency.
- Confirm root input type matches the first task input contract.

## 4. Performance Tuning

- Keep tasks non-blocking (`Mono`-native I/O) whenever possible.
- Avoid heavy CPU work inside Reactor event-loop threads; offload when needed.
- Prefer short, composable tasks over one large multi-purpose task.
- Use `parallel(...)`/`fork(...)` only when branches are truly independent.
- Size retries and timeouts from measured latencies, not guesses.

## 5. Logging Recommendations

Suggested baseline by environment:

- **Local/Dev**:
  - `org.royada.flowforge=DEBUG`
  - app task packages=`DEBUG`
- **Staging**:
  - `org.royada.flowforge=INFO`
  - app task packages=`INFO`
  - temporary `DEBUG` only during investigations
- **Production**:
  - `org.royada.flowforge=INFO`
  - app task packages=`INFO`
  - enable `DEBUG` selectively and time-box it

If you use `AsyncLoggingWorkflowMonitor`, task failures are logged at warning/error depending on level, and debug mode adds thread context for deeper diagnostics.
