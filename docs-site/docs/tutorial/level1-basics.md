---
id: level1-basics
title: "Level 1: The First Step"
sidebar_label: "L1: The Basics"
---

# Level 1: The First Step (The Basics) 🚀

Welcome to the **FlowForge** tutorial! In this level, you will learn the core fundamentals to create your first reactive, type-safe workflow.

---

## 🎯 Goal
Create a workflow that executes a single task: fetching order details (`Order`).

### Key Concepts
1.  **TaskHandler**: A class grouping related tasks together.
2.  **FlowTask**: A method inside a handler representing a single unit of work.
3.  **WorkflowExecutionPlan**: The "map" of how tasks are connected.
4.  **FlowForgeClient**: The engine that triggers executions.

---

## 🏗️ Step 1: Define the Task
In FlowForge, tasks are simple methods annotated with `@FlowTask`. No complex interfaces needed.

```java
import org.royada.flowforge.annotation.TaskHandler;
import org.royada.flowforge.annotation.FlowTask;
import reactor.core.publisher.Mono;

@TaskHandler("order-tasks")
public class OrderTasks {

    @FlowTask(id = "fetchOrder")
    public Mono<Order> fetchOrder(String orderId) {
        // We use the input to search for the specific order
        Order order = new Order(orderId, 99.99);
        return Mono.just(order);
    }
}
```

> [!TIP]
> **Input in First Tasks**: In this example, the first task needs an `orderId` to know what to fetch. Using `Void` as input is also possible for workflows that don't require any initial data from the client, such as a startup cleanup task.

---

## 🗺️ Step 2: Create the Execution Plan
Now we need to tell FlowForge how our workflow looks. We'll use the **FlowForge DSL**.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.royada.flowforge.annotation.FlowWorkflow;
import org.royada.flowforge.dsl.FlowDsl;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

@Configuration
public class OrderFlowConfig {

    @FlowWorkflow(id = "get-order-flow")
    @Bean
    public WorkflowExecutionPlan orderPlan(FlowDsl dsl) {
        // Start the flow with our task method reference
        return dsl.flow(OrderTasks::fetchOrder)
                  .build();
    }
}
```

---

## ▶️ Step 3: Run the Workflow
Finally, use the `FlowForgeClient` to trigger the execution from any service in your app.

```java
import org.springframework.stereotype.Service;
import org.royada.flowforge.api.FlowForgeClient;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private final FlowForgeClient client;

    public OrderService(FlowForgeClient client) {
        this.client = client;
    }

    public Mono<Order> getOrderDetails(String orderId) {
        // Execute the flow by ID, passing the required initial input
        return client.executeResult("get-order-flow", orderId)
                     .cast(Order.class);
    }
}
```

> [!TIP]
> **Casting Results**: When using `executeResult`, you should always cast the result to the output type of the **last task** executed in your workflow plan. In this case, `fetchOrder` returns `Order`, so we cast to `Order.class`.

---

## ✅ Level 1 Checklist
1.  Defined a class with `@TaskHandler`.
2.  Created a task with `@FlowTask` returning a `Mono`.
3.  Designed a plan with `FlowDsl`.
4.  Ran the workflow using the client.

**Congratulations!** You've forged your first flow. In **Level 2**, we'll learn how to connect multiple tasks so data flows automatically between them.

---

**[Next Level: Connecting the Dots (Sequential Flows) >>](./level2-sequential.md)**
