---
title: "Level 4: Resilience"
sidebar_label: "L4: Resilience"
---

# Level 4: Building Resilient Workflows 🛡️

In the real world, systems fail, APIs time out, and networks are unstable. This level is a **hands-on lab** where you will learn to handle these situations like a pro.

---

## 🏗️ The Base Scenario: The "Flaky" Notifier

Let's modify our `NotificationTasks` to simulate an unstable external service that fails **70% of the time**.

```java title="NotificationTasks.java"
@Component
@TaskHandler("notifications")
public class NotificationTasks {

    @FlowTask(id = "notifyResult")
    public Mono<Void> notifyResult(ValidationResult result) {
        // 🧪 Lab Scenario: Randomly fail with a network error
        if (Math.random() < 0.7) {
            System.err.println("⚠ [ERROR] Failed to send notification (Simulated)");
            return Mono.error(new RuntimeException("External Service Timeout!"));
        }
        
        System.out.println("✅ Notification sent successfully!");
        return Mono.empty();
    }
}
```

> [!CAUTION]
> **Try it out!** Run your application and trigger the order process. You'll likely see the whole workflow fail.

---

## 🔧 Challenge 1: The "Persistent" Notifier (Retries)

**Mission**: Don't let a temporary network glitch stop the order. Make the notification try up to **3 times** before giving up.

**Solution**: You don't need to change your logic. Just configure the task:

```java title="Update NotificationTasks.java"
@FlowTask(id = "notifyResult", retryMaxRetries = 3) // 🔁 Add retries here!
public Mono<Void> notifyResult(ValidationResult result) {
    // Same logic...
}
```

**What happens?** FlowForge will catch the error and automatically retry the task. You'll see several error logs in your console, followed by a final success.

---

## ⏲️ Challenge 2: Racing against Time (Timeouts)

**Mission**: The auditing process is taking too long. If it doesn't finish in **500ms**, we want to cancel it and stop waiting.

**Solution**: You can apply policies directly in the Orchestrator for centralized control.

```java title="OrderProcessWorkflow.java"
@Override
public WorkflowExecutionPlan define(FlowDsl dsl) {
    return dsl.flow(OrderTasks::fetchOrder)
              .then(OrderTasks::validateOrder)
              .fork(
                  branch -> branch.then(NotificationTasks::notifyResult)
                                  .withRetry(RetryPolicy.of(3)), // Also possible via DSL!
                  
                  // ⏱ Set a strict deadline for the audit
                  branch -> branch.then(AuditTasks::archiveAuditLog)
                                  .withTimeout(Duration.ofMillis(500)) 
              )
              .join(OrderTasks::finalNotification)
              .build();
}
```

---

## 🛡️ Challenge 3: Keeping the Ship Afloat (Optional Tasks)

**Mission**: The notification is still a bit slow, but it's **not critical**. Even if it fails after all retries or timeouts, we want the order to be processed successfully.

**Solution**: Mark it as **optional**.

```java title="Update NotificationTasks.java"
@FlowTask(id = "notifyResult", retryMaxRetries = 3, optional = true) // 🛡 Mark as optional
public Mono<Void> notifyResult(ValidationResult result) {
    // ...
}
```

**The result**: If the notification fails definitely, FlowForge will log the event as `SKIPPED` or `FAILED`, but it **will not stop** the execution. The workflow will continue to the final join.

## 🛠️ Handling Fatal Errors

If a **Required** task fails even after all retries, FlowForge follows the **Fail-Fast** principle: it stops the entire workflow to avoid inconsistent states.

When this happens, the `client.execute()` or `client.executeResult()` methods will emit an **error signal**. You can handle it in two ways:

1.  **Reactive Way (Recommended)**: Use Reactor operators like `.onErrorResume(e -> ...)` or `.doOnError(e -> ...)` in your business logic.
2.  **Traditional Way**: If you use `.block()`, you must wrap the call in a `try-catch` block:

```java
try {
    client.executeResult("order-process", orderId).block();
} catch (WorkflowExecutionException e) {
    System.err.println("Workflow failed: " + e.getMessage());
    // The causing error is available via e.getCause()
}
```

> [!NOTE]
> **Why choose?** Use **Required** tasks for business-critical steps (like charging a credit card) and **Optional** tasks for "nice-to-have" side effects (como analytics or notifications).

---

## 🎉 Summary of Resilience

You've learned the three pillars of robust orchestration:
1.  **Retries**: Recover from temporary errors.
2.  **Timeouts**: Avoid hanging resources.
3.  **Optional Tasks**: Degrade gracefully when non-critical parts fail.

**Ready for the final level? [Level 5: Advanced Features](./level5-advanced.md) awaits!**
