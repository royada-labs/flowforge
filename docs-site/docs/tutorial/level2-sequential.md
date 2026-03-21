---
title: "Level 2: Sequential"
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
        if (order.getAmount() > 0) {
            return Mono.just(new ValidationResult(true));
        }
        return Mono.just(new ValidationResult(false));
    }
}
```

### Step 2: Chain with `.then()`
We use the `.then()` operator in our plan to connect the blocks using the **Annotation Way**.

```java
@Component
@FlowWorkflow(id = "order-process")
public class OrderProcessWorkflow implements WorkflowDefinition {

    @Override
    public WorkflowExecutionPlan define(FlowDsl dsl) {
        return dsl.flow(OrderTasks::fetchOrder) // Returns Order
                  .then(OrderTasks::validateOrder) // Receives Order, Returns ValidationResult
                  .build();
    }
}
```

### Step 3: Update the Service
Since the last task in our workflow is now `validateOrder`, the `FlowForgeClient` will return a `ValidationResult`. Also, as our business logic is growing beyond a simple "fetch", we will rename the service method to **`processOrder`** to better reflect the new reality.

```java
@Service
public class OrderService {
    private final FlowForgeClient client;

    public OrderService(FlowForgeClient client) {
        this.client = client;
    }

    public Mono<ValidationResult> processOrder(String orderId) {
        // Now it returns ValidationResult because it's the last task in the plan
        return client.executeResult("order-process", orderId)
                     .cast(ValidationResult.class);
    }
}
```

> [!IMPORTANT]
> **Type Safety**: If you tried to connect a task returning a `String` to another expecting an `Integer`, FlowForge **will fail during startup**, warning you of the mistake before it reaches production.


> [!TIP]
> **Check the Code**: You can see the full source code for this level in our Sample Repository using the tag:
> ```bash
> git checkout level-2
> ```


**[Next Level: Divide and Conquer (Parallelism) >>](./level3-parallel.md)**
