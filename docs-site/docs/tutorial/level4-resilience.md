---
title: "Level 4: Resilience"
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


> [!TIP]
> **Check the Code**: You can see the full source code for this level in our Sample Repository using the tag:
> ```bash
> git checkout level-4
> ```


**[Next Level: Mastering the Flow (Advanced Features) >>](./level5-advanced.md)**
