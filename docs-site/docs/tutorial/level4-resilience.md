---
id: level4-resilience
title: "Level 4: Resilience at Scale"
sidebar_label: "L4: Resilience"
---

# Level 4: Resilience (Retries and Timeouts) 🛡️

In the real world, external services fail. FlowForge allows you to handle these failures without cluttering your business logic.

## 🎯 Goal
Configure a retry policy for an unstable payment gateway and a timeout limit for a slow legacy task.

### Option A: Via Annotations (Recommended)
You can define resilience directly on the task itself.

```java
@FlowTask(
    id = "processPayment",
    retryMaxRetries = 3,         // Retry up to 3 times
    retryBackoffMillis = 1000,   // Wait 1 second between retries
    timeoutMillis = 5000         // Limit the task to 5 seconds
)
public Mono<PaymentStatus> processPayment(Order order) { ... }
```

### Option B: Via DSL
If you prefer to keep the task "clean" and configure resilience in the plan:

```java
return dsl.flow(tasks::processPayment)
          .withRetry(RetryPolicy.backoff(3, Duration.ofSeconds(1)))
          .withTimeout(Duration.ofSeconds(5))
          .build();
```

---

## ✅ Level 4 Checklist
1.  Applied a `RetryPolicy` to a transient service.
2.  Set a `timeout` for potentially slow operations.
3.  Isolated failure handling from business logic.

**Almost there!** In **Level 5**, we'll learn about advanced features like execution context and optional paths.

---

**[Next Level: Mastering the Flow (Advanced Features) >>](./level5-advanced.md)**
