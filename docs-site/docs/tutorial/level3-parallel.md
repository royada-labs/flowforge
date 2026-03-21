---
title: "Level 3: Parallel"
sidebar_label: "L3: Parallel"
---




# Level 3: Divide and Conquer (Parallelism) ⚡

Why wait for an email to be sent before updating stock? In this level, you will learn to execute independent tasks in parallel.

## 🎯 Goal
After validating the order, we want to perform two operations simultaneously: notify the user and archive the validation result for auditing.

### Step 1: Define the Parallel Tasks
We'll create two new components that react to the `ValidationResult`. Notice how they don't depend on the original `Order` object, keeping the logic decoupled.

```java
@Component
@TaskHandler("order-tasks")
public class OrderTasks {
    // ... previous tasks ...

    @FlowTask(id = "finalNotification")
    public Mono<Void> finalNotification(Object in) {
        // This task runs AFTER the parallel branches are joined
        System.out.println("Pipeline finished. Sending final system alert.");
        return Mono.empty();
    }
}

@Component
@TaskHandler("notifications")
public class NotificationTasks {
    @FlowTask(id = "notifyResult")
    public Mono<Void> notifyResult(ValidationResult in) {
        // Simulate a slow notification system (1 second delay)
        System.out.println("Notifying customer of result: " + in.isValid());
        return Mono.empty()
                   .delayElement(Duration.ofSeconds(1))
                   .then();
    }
}

@Component
@TaskHandler("audit")
public class AuditTasks {
    @FlowTask(id = "archiveAuditLog")
    public Mono<Void> archiveAuditLog(ValidationResult in) {
        // Fast audit archiving logic
        System.out.println("Validation result archived: " + in.getReason());
        return Mono.empty();
    }
}
```

### Step 2: The Parallel Plan
Use the `.fork()` operator to open parallel branches and `.join()` to wait for both audit and notification to finish.

```java
@Component
@FlowWorkflow(id = "order-process")
public class OrderProcessWorkflow implements WorkflowDefinition {

    @Override
    public WorkflowExecutionPlan define(FlowDsl dsl) {
        return dsl.flow(OrderTasks::fetchOrder)
                  .then(OrderTasks::validateOrder)
                  .fork(
                      // Branch A: Slow Notification
                      branch -> branch.then(NotificationTasks::notifyResult),
                      // Branch B: Fast Audit Log
                      branch -> branch.then(AuditTasks::archiveAuditLog)
                  )
                  .join(OrderTasks::finalNotification)
                  .build();
    }
}
```

### Step 3: Update the Service
When your plan ends with a `.join()`, the return type of the service method must match the output of the task passed to the join method. In this case, `finalNotification` returns `Void`.

```java
@Service
public class OrderService {
    private final FlowForgeClient client;

    public OrderService(FlowForgeClient client) {
        this.client = client;
    }

    public Mono<Void> processOrder(String orderId) {
        // Since finalNotification returns Void, we get a Mono<Void> here
        return client.executeResult("order-process", orderId)
                     .then();
    }
}
```

### How does Join work?
The `finalNotification` task will execute only when both parallel branches have finished successfully.


> [!TIP]
> **Check the Code**: You can see the full source code for this level in our Sample Repository using the tag:
> ```bash
> git checkout level-3
> ```


**[Next Level: Resilience at Scale (Retries and Timeouts) >>](./level4-resilience.md)**
