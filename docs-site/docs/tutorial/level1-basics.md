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
>
> If your final task returns `Void` (`Mono<Void>`), the resulting `Mono<Object>` will complete by emitting `null`. In that case, you can cast to `Void.class` or simply use `.then()` to trigger subsequent logic.


**[Next Level: Connecting the Dots (Sequential Flows) >>](./level2-sequential.md)**
