# Real-World Examples

The following examples demonstrate how to use FlowForge in production scenarios.

## 1. User Onboarding Flow (Annotation-First)

**Context**: When a user signs up, we need to create their account, send a welcome email, and provision a free tier resource.

```java
@Configuration
public class OnboardingFlow {
    @TaskHandler("onboarding")
    static class OnboardingTasks {
        @FlowTask(id = "createAccount")
        Mono<User> createAccount(Void in) { ... }

        @FlowTask(id = "sendWelcomeEmail")
        Mono<Void> sendWelcomeEmail(User in) { ... }

        @FlowTask(id = "provisionResource")
        Mono<Void> provisionResource(User in) { ... }

        @FlowTask(id = "notifySlackAdmin")
        Mono<Void> notifySlackAdmin(Object in, ReactiveExecutionContext ctx) { ... }
    }

    @FlowWorkflow(id = "user-onboarding")
    @Bean
    public WorkflowExecutionPlan plan(FlowDsl dsl) {
        return dsl.flow(OnboardingTasks::createAccount)
                  .fork(
                      branch -> branch.then(OnboardingTasks::sendWelcomeEmail),
                      branch -> branch.then(OnboardingTasks::provisionResource)
                  )
                  .join(OnboardingTasks::notifySlackAdmin)
                  .build();
    }
}
```

**Behavior**: 
- `CREATE_ACCOUNT` runs first. 
- Once finished, `SEND_WELCOME_EMAIL` and `PROVISION_RESOURCE` run in parallel.
- `NOTIFY_SLACK_ADMIN` only runs after both parallel branches succeed.
- `ReactiveExecutionContext` is injected only in tasks that need it (`notifySlackAdmin` in this example).

---

## 2. API Orchestration (Annotation-First)

**Context**: A gateway service that aggregates data from two microservices (User and Order) and calculates a discount.

```java
@TaskHandler
class PricingTasks {
    @FlowTask(id = "start")
    Mono<Object> start(Void in, ReactiveExecutionContext ctx) { ... }
    @FlowTask(id = "getUser")
    Mono<Object> getUser(Object in, ReactiveExecutionContext ctx) { ... }
    @FlowTask(id = "getOrder")
    Mono<Object> getOrder(Object in, ReactiveExecutionContext ctx) { ... }
    @FlowTask(id = "calc")
    Mono<Double> calc(Object in, ReactiveExecutionContext ctx) { ... }
}

dsl.flow(PricingTasks::start)
   .parallel(PricingTasks::getUser, PricingTasks::getOrder)
   .join(PricingTasks::calc)
   .build();
```

Alternative for advanced use-cases: explicit `TaskDefinition` contracts are still supported.

Overloaded method references are supported and resolved by full signature:

```java
TaskCallRef<MyTasks, Integer, String> intMapper = MyTasks::map;
TaskCallRef<MyTasks, Long, String> longMapper = MyTasks::map;
```

---

## 3. Data Transformation Pipeline

**Context**: Ingesting a raw JSON string, validating it, transforming it to a canonical format, and saving it.

```java
dsl.startTyped(VALIDATE_JSON)
   .then(MAP_TO_CANONICAL)
   .then(PERSIST)
   .build();
```

---

## 4. Error Handling & Policies

You can make workflows resilient by combining execution timeout at client level with task policies.

```java
client.execute("resilient-flow", null, Duration.ofMillis(500));
```

**Behavior**:
- The workflow execution is canceled if it takes longer than 500ms.
- Task-level retry/timeout policies are configured in core task descriptors (`RetryPolicy`, `TimeoutPolicy`).

A task descriptor example (used internally by the framework) looks like this:

```java
TaskDescriptor descriptor = new TaskDescriptor(task, RetryPolicy.fixed(3));
```

This ensures retries/timeout are applied to the task at runtime.

---

## 5. Retry Exhausted Example

**Context**: A downstream dependency remains unavailable beyond the retry budget.

```java
TaskDescriptor descriptor = new TaskDescriptor(task, RetryPolicy.fixed(3));
```

**Observed behavior**:
- The task runs once and then retries up to 3 additional attempts.
- If all attempts fail, the final error is propagated and the branch fails.
- Dependent tasks are not executed unless modeled as optional/fallback paths.

---

## 6. Timeout Example

**Context**: A task calls a slow API and must fail fast after 2 seconds.

```java
TaskDescriptor descriptor = new TaskDescriptor(task, TimeoutPolicy.of(Duration.ofSeconds(2)));
```

**Observed behavior**:
- If the task takes longer than 2 seconds, it fails with timeout.
- This is task-level timeout and is independent from client-level execution timeout.
- Use `client.execute(..., timeout)` when you need a global cap for the entire workflow run.

---

## 7. Optional Tasks Example

**Context**: A best-effort notification should not block order finalization.

```java
@TaskHandler
class CheckoutTasks {
    @FlowTask(id = "chargeCard")
    Mono<String> chargeCard(Void in) { ... }

    @FlowTask(id = "sendReceipt", optional = true)
    Mono<Void> sendReceipt(String paymentId) { ... }

    @FlowTask(id = "finalizeOrder")
    Mono<String> finalizeOrder(String paymentId) { ... }
}

dsl.flow(CheckoutTasks::chargeCard)
   .then(CheckoutTasks::sendReceipt)
   .then(CheckoutTasks::finalizeOrder)
   .build();
```

**Observed behavior**:
- If `sendReceipt` fails, it can be skipped because it is optional.
- Critical tasks continue according to dependency constraints.
- Use optional tasks for non-critical side effects such as notifications/analytics.
