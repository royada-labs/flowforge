---
title: "Level 1: The Basics"
sidebar_label: "L1: Basics"
---




# Level 1: The First Step (The Basics) 🚀

Welcome to the **FlowForge** tutorial! In this level, you will learn the core fundamentals to create your first reactive, type-safe workflow.


## 🏗️ Step 1: Define the Task
In FlowForge, tasks are simple methods annotated with `@FlowTask`. No complex interfaces needed.

```java
import org.royada.flowforge.annotation.TaskHandler;
import org.royada.flowforge.annotation.FlowTask;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
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


## 🔗 Step 2: Orchestrate the Flow
Once the tasks are ready, define your workflow using the **`@FlowWorkflow`** annotation. This allows FlowForge to automatically discover and register your blueprint.

```java
import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.workflow.WorkflowDefinition;
import org.royada.flowforge.spring.dsl.FlowDsl;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.springframework.stereotype.Component;

@Component
@FlowWorkflow(id = "order-process")
public class OrderProcessWorkflow implements WorkflowDefinition {

    @Override
    public WorkflowExecutionPlan define(FlowDsl dsl) {
        // High-level orchestration for the E-Commerce pipeline
        return dsl.flow(OrderTasks::fetchOrder)
                  .build();
    }
}
```

> [!TIP]
> **Component Scanning**: By using `@Component` and `@FlowWorkflow`, you don't need to manually register Beans. FlowForge will find your workflow at startup.


## ▶️ Step 3: Run the Workflow
Finally, use the `FlowForgeClient` to trigger the execution from any service in your app.

```java
@Service
public class OrderService {

    private final FlowForgeClient client;

    public OrderService(FlowForgeClient client) {
        this.client = client;
    }

    public Mono<Order> getOrderDetails(String orderId) {
        // Execute the "order-process" flow
        return client.executeResult("order-process", orderId)
                     .cast(Order.class);
    }
}
```

> [!TIP]
> **Casting Results**: When using `executeResult`, you should always cast the result to the output type of the **last task** executed in your workflow plan. In this case, `fetchOrder` returns `Order`, so we cast to `Order.class`.
>
> If your final task returns `Void` (`Mono<Void>`), the resulting `Mono<Object>` will complete by emitting `null`. In that case, you can cast to `Void.class` or simply use `.then()` to trigger subsequent logic.


> [!TIP]
> **Check the Code**: You can see the full source code for this level in our Sample Repository using the tag:
> ```bash
> git checkout level-1
> ```


**[Next Level: Connecting the Dots (Sequential Flows) >>](./level2-sequential.md)**
