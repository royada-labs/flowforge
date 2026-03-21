---
title: "Level 5: Advanced"
sidebar_label: "L5: Advanced"
---




# Level 5: Mastering the Flow (Advanced Features) 🧠

You've made it! This level will show you the pro-tricks for building truly complex production workflows.

## 🧰 1. Using the Execution Context
Sometimes you need to access data that was generated five steps ago. Instead of passing it from task to task, use the `ReactiveExecutionContext`.

```java
@FlowTask(id = "finalize")
public Mono<Void> finalize(Object in, ReactiveExecutionContext ctx) {
    // Retrieve the Order generated at the beginning
    Order order = ctx.getOrThrow(FETCH_ORDER_KEY); 
    System.out.println("Finalizing order: " + order.getOrderId());
    return Mono.empty();
}
```

## 🍃 2. Optional Tasks
Do you have an analytics step that isn't critical? If it fails, you don't want to stop the whole workflow.

```java
@FlowTask(id = "trackAnalytics", optional = true)
public Mono<Void> trackAnalytics(Order in) {
    // If this Mono returns an error, the main workflow CONTINUES.
}
```

---

## 🎉 Congratulations!
You've completed the FlowForge Tutorial. You now have the skills to build reactive, robust, and type-safe workflows for any application.

> [!TIP]
> **Check the Code**: You can see the full source code for this level in our Sample Repository using the tag:
> ```bash
> git checkout level-5
> ```


**Explore more in the [API Reference](../../api-reference/index.md) or look at the [Examples](../../examples/index.md) section.**
