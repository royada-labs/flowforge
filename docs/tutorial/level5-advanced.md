---
id: level5-advanced
title: "Level 5: Mastering the Flow"
sidebar_label: "L5: Advanced"
---

# Level 5: Mastering the Flow (Advanced Features) 🧠

You've made it! This level will show you the pro-tricks for building truly complex production workflows.

## 🧰 1. Using the Execution Context
Sometimes you need to access data that was generated five steps ago. Instead of passing it from task to task, use the `ReactiveExecutionContext`.

```java
@FlowTask(id = "finalize")
public Mono<Void> finalize(Object in, ReactiveExecutionContext ctx) {
    // Retrieve the OrderId generated at the beginning
    String orderId = ctx.getOrThrow(FETCH_ORDER_KEY); 
    System.out.println("Finalizing order: " + orderId);
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

## 🎉 Congratulations, FlowForge Master!
You have completed the tutorial. You now have all the tools to build reactive, typed, and extremely robust systems.

### What's Next?
- Check the [DSL Reference](../api-reference/index.md).
- Explore the [Observability Guide](../observability/index.md).
- Join the community and contribute!
