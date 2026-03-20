# Migrating from Deprecated Constructors to Builder Pattern

## Overview

In version 1.2.0, the deprecated constructors in `ReactiveWorkflowOrchestrator` have been removed. Previously, these constructors were marked as `@Deprecated` but still functional. As of this version, **only the Builder pattern is supported**.

## What Changed?

### Before (Deprecated - No Longer Works)

```java
// ❌ Old way - constructor with defaults
ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

// ❌ Constructor with custom monitor and resolver
ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
    Schedulers.parallel(),
    new NoOpWorkflowMonitor(),
    new DefaultTaskInputResolver()
);

// ❌ Constructor with all parameters (5 args)
ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
    Schedulers.parallel(),
    Schedulers.newSingle("state"),
    new NoOpWorkflowMonitor(),
    new DefaultTaskInputResolver(),
    10  // maxConcurrency
);
```

### After (Current - Builder Pattern)

```java
// ✅ Default configuration (most common)
ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder().build();

// ✅ Custom task scheduler and other components
ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder()
    .taskScheduler(Schedulers.parallel())
    .monitor(new NoOpWorkflowMonitor())
    .inputResolver(new DefaultTaskInputResolver())
    .build();

// ✅ Full configuration with limits
ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder()
    .taskScheduler(Schedulers.parallel())
    .stateScheduler(Schedulers.newSingle("state"))
    .monitor(new NoOpWorkflowMonitor())
    .inputResolver(new DefaultTaskInputResolver())
    .limits(new ExecutionLimits(10, 1000, BackpressureStrategy.BLOCK))
    .build();
```

## Migration Steps

### Step 1: Identify Usage

Search your codebase for:
```bash
grep -r "new ReactiveWorkflowOrchestrator(" src/
```

### Step 2: Replace Constructor Calls

| Old Constructor | New Builder Equivalent |
|-----------------|------------------------|
| `new ReactiveWorkflowOrchestrator()` | `ReactiveWorkflowOrchestrator.builder().build()` |
| `new ReactiveWorkflowOrchestrator(taskScheduler, monitor, resolver)` | `.taskScheduler(...).monitor(...).inputResolver(...).build()` |
| `new ReactiveWorkflowOrchestrator(taskScheduler, stateScheduler, monitor, resolver, maxConcurrency)` | `.taskScheduler(...).stateScheduler(...).monitor(...).inputResolver(...).limits(new ExecutionLimits(maxConcurrency, 1000, BackpressureStrategy.BLOCK)).build()` |
| `new ReactiveWorkflowOrchestrator(taskScheduler, stateScheduler, monitor, resolver, limits)` | `.taskScheduler(...).stateScheduler(...).monitor(...).inputResolver(...).limits(limits).build()` |
| `new ReactiveWorkflowOrchestrator(taskScheduler, stateScheduler, monitor, resolver, tracerFactory, limits)` | `.taskScheduler(...).stateScheduler(...).monitor(...).inputResolver(...).tracerFactory(...).limits(limits).build()` |

### Step 3: Add Missing Imports

If you use `ExecutionLimits` or `BackpressureStrategy`, add these imports:

```java
import org.royada.flowforge.workflow.orchestrator.BackpressureStrategy;
import org.royada.flowforge.workflow.orchestrator.ExecutionLimits;
```

### Step 4: Verify Behavior

The Builder pattern produces the **same runtime behavior** as the deprecated constructors. The only difference is the API style:

- All defaults remain the same:
  - `taskScheduler` defaults to `Schedulers.boundedElastic()`
  - `stateScheduler` defaults to `Schedulers.newSingle("flowforge-state")`
  - `monitor` defaults to `NoOpWorkflowMonitor`
  - `inputResolver` defaults to `DefaultTaskInputResolver`
  - `tracerFactory` defaults to `typeMetadata -> new NoOpExecutionTracer()`
  - `limits` defaults to `ExecutionLimits.defaultLimits()`

### Step 5: Run Tests

Ensure your workflow tests still pass after migration.

## Common Migration Examples

### Example 1: Simple Test Setup

**Before:**
```java
@Test
void testWorkflow() {
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();
    // ...
}
```

**After:**
```java
@Test
void testWorkflow() {
    ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder().build();
    // ...
}
```

### Example 2: Custom Schedulers for Performance Tests

**Before:**
```java
ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
    Schedulers.parallel(),
    Schedulers.newSingle("state"),
    new NoOpWorkflowMonitor(),
    new DefaultTaskInputResolver(),
    maxConcurrency
);
```

**After:**
```java
ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder()
    .taskScheduler(Schedulers.parallel())
    .stateScheduler(Schedulers.newSingle("state"))
    .monitor(new NoOpWorkflowMonitor())
    .inputResolver(new DefaultTaskInputResolver())
    .limits(new ExecutionLimits(maxConcurrency, 1000, BackpressureStrategy.BLOCK))
    .build();
```

### Example 3: Custom Monitor

**Before:**
```java
ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
    Schedulers.immediate(),
    customMonitor,
    new DefaultTaskInputResolver()
);
```

**After:**
```java
ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder()
    .taskScheduler(Schedulers.immediate())
    .monitor(customMonitor)
    .inputResolver(new DefaultTaskInputResolver())
    .build();
```

## Why the Change?

The Builder pattern provides:

1. **Better readability** - Fluent API with named setter methods
2. **Extensibility** - Easy to add new configuration options without breaking binary compatibility
3. **Optional parameters** -Natural handling of optional configuration (use defaults)
4. **Immutability** - The builder is mutable during construction, but the resulting `ReactiveWorkflowOrchestrator` is immutable
5. **Consistency** - Same pattern used throughout modern Java libraries

## Need Help?

If you encounter issues during migration:

1. Check the [API Reference](../api-reference/index.md) for `ReactiveWorkflowOrchestrator.Builder`
2. Review the [Core Concepts](../core-concepts/index.md) on execution model
3. Open an [issue](https://github.com/flowforge/flowforge/issues) on GitHub

---

**Related Changes:**
- Removed all `@Deprecated` constructors from `ReactiveWorkflowOrchestrator`
- All existing tests migrated to builder pattern
- No runtime behavior changes — purely API cleanup
