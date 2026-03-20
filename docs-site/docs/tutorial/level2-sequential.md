---
id: level2-sequential
title: "Level 2: Connecting the Dots"
sidebar_label: "L2: Sequential"
---

# Level 2: Sequential Flows 🔗

Most workflows don't end in one step. In this level, we'll connect two tasks to make data flow automatically between them.

## 🎯 Goal
Make the workflow fetch an `Order` and then validate it (`ValidationResult`).

### Step 1: Add the Validation Task
We'll add a new method to our `OrderTasks`. Notice that the result of `fetchOrder` is the input for `validateOrder`.

```java
@TaskHandler("order-tasks")
public class OrderTasks {

    @FlowTask(id = "fetchOrder")
    public Mono<Order> fetchOrder(String orderId) { ... }

    @FlowTask(id = "validateOrder")
    public Mono<ValidationResult> validateOrder(Order order) {
        // FlowForge passes the Order object from the previous task here
        if (order.amount() > 0) {
            return Mono.just(new ValidationResult(true));
        }
        return Mono.just(new ValidationResult(false));
    }
}
```

### Step 2: Chain with `.then()`
We use the `.then()` operator in our plan to connect the blocks.

```java
@FlowWorkflow(id = "order-validation-flow")
@Bean
public WorkflowExecutionPlan orderPlan(FlowDsl dsl) {
    return dsl.flow(OrderTasks::fetchOrder) // Returns Order
              .then(OrderTasks::validateOrder) // Receives Order, Returns ValidationResult
              .build();
}
```

> [!IMPORTANT]
> **Type Safety**: If you tried to connect a task returning a `String` to another expecting an `Integer`, FlowForge **will fail during startup**, warning you of the mistake before it reaches production.

---

## ✅ Level 2 Checklist
1.  Connected two tasks sequentially using `.then()`.
2.  Observed automatic data propagation.
3.  Learned about build-time type validation.

**Well done!** You're moving fast. In **Level 3**, we'll learn how to execute independent tasks concurrently.

---

**[Next Level: Divide and Conquer (Parallelism) >>](./level3-parallel.md)**
