# Annotations Reference

FlowForge uses a set of specialized annotations to enable a **Code-First** experience in Spring Boot applications. This guide describes each annotation and its parameters.

---

## `@TaskHandler`
Marks a Spring `@Component` or `@Bean` as a container for workflow tasks.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `value` | `String` | `""` | **Optional logical group tag.** Used for documentation, dashboards, and logical organization in the Task Registry. It does **not** prefix the task IDs. |

### Example
```java
@Component
@TaskHandler("ecommerce-logic")
public class OrderTasks { ... }
```

---

## `@FlowTask`
Applied to methods within a `@TaskHandler` class to register them as steps in a workflow.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `id` | `String` | **Required** | The unique technical identifier for this task in the registry. |
| `retryMaxRetries` | `int` | `-1` | Maximum number of retry attempts on failure. |
| `retryBackoffMillis` | `long` | `-1` | Wait time (ms) between retries (exponential backoff). |
| `timeoutMillis` | `long` | `-1` | Maximum execution time (ms) allowed for this task. |

### Example
```java
@FlowTask(
    id = "sendEmail", 
    retryMaxRetries = 3, 
    timeoutMillis = 5000
)
public Mono<Void> sendEmail(Order order) { ... }
```

---

## `@FlowWorkflow`
Registers a class (implementing `WorkflowDefinition`) as a discoverable workflow blueprint.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `id` | `String` | **Required** | The unique identifier used to trigger this workflow via `FlowForgeClient`. |

### Example
```java
@Component
@FlowWorkflow(id = "order-process")
public class OrderProcessWorkflow implements WorkflowDefinition { ... }
```
