---
title: "Level 3: Parallel"
sidebar_label: "L3: Parallel"
---




# Level 3: Divide and Conquer (Parallelism) ⚡

Why wait for an email to be sent before updating stock? In this level, you will learn to execute independent tasks in parallel.

## 🎯 Goal
After validating the order, we'll send an email AND update inventory at the same time. Finally, we'll notify the system.

### The Orchestration Plan
Use the `.fork()` operator to open parallel branches and `.join()` to wait for all of them to finish.

```java
@FlowWorkflow(id = "parallel-processing-flow")
@Bean
public WorkflowExecutionPlan orderPlan(FlowDsl dsl) {
    return dsl.flow(OrderTasks::fetchOrder)
              .then(OrderTasks::validateOrder)
              .fork(
                  // Branch A: Email
                  branch -> branch.then(NotificationTasks::emailCustomer),
                  // Branch B: Inventory
                  branch -> branch.then(InventoryTasks::updateStock)
              )
              .join(OrderTasks::finalNotification)
              .build();
}
```

### How does Join work?
The `finalNotification` task will execute only when both parallel branches have finished successfully.


**[Next Level: Resilience at Scale (Retries and Timeouts) >>](./level4-resilience.md)**
