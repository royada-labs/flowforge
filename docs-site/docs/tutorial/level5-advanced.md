# Level 5: Professional Workflows (Context & Decoupling)

By now, you have a robust parallel workflow with retries and timeouts. But in the real world, **not all tasks are equal**. Some are critical (like charging a payment), others are "best-effort" (like a marketing notification).

In this final level, we'll learn how to make our workflows truly intelligent using **Optional Tasks** and the **Execution Context**.

---

## 🛡️ Challenge 1: The Safety Net (Optional Tasks)

Remember in **Level 4** when our slow `archiveAuditLog` crashed the whole order because of a timeout? If auditing is important but **not critical**, we shouldn't stop the world.

**Mission**: Mark the audit task so its failure (or timeout) is ignored by the orchestrator.

1.  **Modify your `AuditTasks.java`**:

```java title="Update AuditTasks.java"
@Component
@TaskHandler("audit")
public class AuditTasks {

    @FlowTask(
        id = "archiveAuditLog", 
        optional = true // 🛡️ Lifeboat! Failure won't stop the workflow
    )
    public Mono<Void> archiveAuditLog(ValidationResult result) {
        return Mono.delay(Duration.ofMillis(1000)) // Still slow (1s)
                .then();
    }
}
```

2.  **Run the demo**. Even though the console shows a `TimeoutException` (because of our 500ms timeout), the workflow will now **complete successfully**. 

---

## 🧠 Challenge 2: Deep Context (Decoupled Data Sharing)

In a traditional Spring application, you might use `ThreadLocal` to pass data around. But in a **Reactive System**, threads are shared and reused constantly! You can't rely on `ThreadLocal`.

### What is the `ReactiveExecutionContext`?
It is the **Shared Memory** of your workflow. It follows the execution across different threads and provides:
1.  **Thread-Safe Storage**: Put and get data without worrying about concurrency.
2.  **Implicit Results**: When any task (like `validateOrder`) finishes, its result is automatically stored in the context using its ID as the key.

### Type-Safe "Reverse Resolution"
To get data from the context without using "magic strings" and without forcing producers and consumers to know each other, the **Consumer** declares its dependency.

**Mission**: Have the `notifyResult` task read the result from `validateOrder` in a 100% type-safe and decoupled way.

1.  **Update your `NotificationTasks.java`**:

```java title="Update NotificationTasks.java"
@Component
@TaskHandler("notifications")
public class NotificationTasks {

    // 🎯 DECLARE locally what we expect to find in the context.
    // We don't need to import the OrderTasks class! We just define the "contract" we need.
    private static final TaskDefinition<Order, ValidationResult> VALIDATION_RESULT = 
        TaskDefinition.of("validateOrder", Order.class, ValidationResult.class);

    @FlowTask(id = "notifyResult")
    public Mono<Void> notifyResult(Order order, ReactiveExecutionContext ctx) {
        
        // 🔍 Retrieve the result produced by ANOTHER task using our local contract.
        // ctx.get() uses the TaskDefinition to ensure type-safety (no casts needed).
        ValidationResult res = ctx.get(VALIDATION_RESULT.outputKey())
                                .orElseThrow(() -> new IllegalStateException("Validation result not found!"));

        if (res.isValid()) {
            System.out.println("💌 Order valid! Sending notification...");
        }
        return Mono.empty();
    }
}
```

:::tip 💡 Pro Tip: Stop using Raw Strings
In large projects, manually writing `"validateOrder"` in multiple files is risky. A professional pattern is to use a **Shared Enum** or a centralized constants class for both the `@FlowTask` ID and the `TaskDefinition` identifier. This ensures uniformity and makes refactoring much easier!
:::

2.  **Why is this architecture powerful?**
*   **Zero Coupling**: `OrderTasks` (the producer) has no idea who is consuming its data.
*   **Explicit Dependencies**: Just by looking at the constants in a task class, you know exactly what input it expects from the context.
*   **Compile-time Safety**: FlowForge handles the internal mapping based on IDs, but your code remains strictly typed.

---

## 🎉 Congratulations!

You've mastered the core of FlowForge! 

1.  **Level 1**: Bootstrapping.
2.  **Level 2**: Sequential logic.
3.  **Level 3**: Concurrency with Parallel/Fork.
4.  **Level 4**: Resilience with Retries/Timeouts.
5.  **Level 5**: Advanced Context & Decoupling.

You are now ready to build complex, reliable, and high-performance reactive workflows. 

---

[⬅ Level 4: Resilience](./level4-resilience.md) | [Back to Reference](../api-reference/index.md)
