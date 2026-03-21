# Level 4: Resilient Workflows (Retries & Timeouts)

At this level, we address one of the most common issues in distributed systems: **instability**. What happens if an external notification service is down? Or if the auditing process is too slow?

FlowForge provides a suite of **Resilience Policies** to handle these failures gracefully without cluttering your business logic.

---

## 🏗️ The Base Scenario: A Flaky Service

Imagine our `notifyResult` task depends on an unstable external API. Sometimes it works, but 70% of the time, it throws a `RuntimeException`.

1.  **Modify your `NotificationTasks.java`** to simulate this instability:

```java title="NotificationTasks.java"
@Component
@TaskHandler("notifications")
public class NotificationTasks {

    @FlowTask(id = "notifyResult")
    public Mono<Void> notifyResult(ValidationResult result) {
        // 🎲 70% chance of failure!
        if (Math.random() < 0.7) {
            System.err.println("⚠ [ERROR] Failed to send notification (Simulated)");
            return Mono.error(new RuntimeException("External Service Timeout!"));
        }
        
        System.out.println("💌 Notification sent successfully!");
        return Mono.empty();
    }
}
```

2.  **Run the demo**. You'll likely see the workflow crash with an exception.

---

## 🔁 Challenge 1: The Persistent Notifier (Retries)

**Mission**: We don't want to fail just because of a transient network glitch. We want to **retry up to 3 times** before giving up.

**Solution**: You can configure retries directly in the annotation.

```java title="Update NotificationTasks.java"
@FlowTask(id = "notifyResult", retryMaxRetries = 3) // 🔁 Add retries here!
public Mono<Void> notifyResult(ValidationResult result) {
    // Same logic...
}
```

**What happens?** FlowForge will catch the error and automatically retry the task. You'll see several error logs in your console, followed by a final success.

> [!TIP]
> **🎲 Resilience is a numbers game!**
> Since we use `Math.random() < 0.7`, there is a 70% chance of failure per attempt. Even with 3 retries (4 total attempts), there's a small (~24%) chance it fails 4 times in a row. 
> If you still see a `WorkflowExecutionException`, just **run the demo again** or increase `retryMaxRetries` until your "luck" improves!

---

## ⏲️ Challenge 2: Racing against Time (Timeouts)

**Mission**: The auditing process is taking too long. If it doesn't finish in **500ms**, we want to cancel it and stop waiting for safety.

1.  **Create a slow task** in a new file `AuditTasks.java`:

```java title="AuditTasks.java"
@Component
@TaskHandler("audit")
public class AuditTasks {

    @FlowTask(id = "archiveAuditLog")
    public Mono<Void> archiveAuditLog(ValidationResult result) {
        return Mono.delay(Duration.ofMillis(1000)) // 🐌 Simulated slowness (1s)
                .doOnTerminate(() -> System.err.println("Audit process interrupted?"))
                .then();
    }
}
```

2.  **Apply the timeout** in your `OrderProcessWorkflow.java`:

```java title="OrderProcessWorkflow.java"
@Override
public WorkflowExecutionPlan define(FlowDsl dsl) {
    return dsl.flow(OrderTasks::fetchOrder)
              .then(OrderTasks::validateOrder)
              .fork(
                  branch -> branch.then(NotificationTasks::notifyResult)
                                  .withRetry(RetryPolicy.fixed(3)),
                  
                  // ⏱ Set a strict deadline for the audit
                  branch -> branch.then(AuditTasks::archiveAuditLog)
                                  .withTimeout(Duration.ofMillis(500)) 
              )
              .build();
}
```

**What happens?** Since `archiveAuditLog` takes 1000ms but we only wait 500ms, the workflow will **fail with a `TimeoutException`**. This is correct for business-critical steps!

---

## 🛠️ Handling Fatal Errors

By default, if a **Required** task fails even after all retries, or hits a timeout, FlowForge follows the **Fail-Fast** principle: it stops the entire workflow to avoid inconsistent states.

When this happens, the `client.execute()` or `client.executeResult()` methods will emit an **error signal**. You can handle it in two ways:

1.  **Reactive Way (Recommended)**: Use Reactor operators like `.onErrorResume(e -> ...)` or `.doOnError(e -> ...)` in your business logic.
2.  **Traditional Way**: If you use `.block()`, you must wrap the call in a `try-catch` block:

```java
try {
    client.executeResult("order-process", orderId).block();
} catch (WorkflowExecutionException e) {
    System.err.println("Workflow failed: " + e.getMessage());
}
```

> [!IMPORTANT]
> **🚀 Next Steps: What if the task is NOT critical?**
> If you don't want a slow notification or a failed log to break your entire order process, you need **Optional Tasks**. We will learn how to handle partial results and non-critical steps in **Level 5: Advanced Features**.

---

## 🎉 Summary of Level 4

1.  **Retries**: Auto-recovery for transient errors (random failures, network blips).
2.  **Timeouts**: Setting strict boundaries for slow external systems.
3.  **Fail-Fast**: Understanding how FlowForge protects your state by stopping on fatal errors.

---

[⬅ Level 3: Parallel & Forking](./level3-parallel.md) | [Level 5: Advanced Features ➡](./level5-advanced.md)
