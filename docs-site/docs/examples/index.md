---
title: "Examples"
sidebar_label: "Examples"
---




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


## 3. Data Transformation Pipeline

**Context**: Ingesting a raw JSON string, validating it, transforming it to a canonical format, and saving it.

```java
dsl.startTyped(VALIDATE_JSON)
   .then(MAP_TO_CANONICAL)
   .then(PERSIST)
   .build();
```


## 5. Retry Exhausted Example

**Context**: A downstream dependency remains unavailable beyond the retry budget.

```java
TaskDescriptor descriptor = new TaskDescriptor(task, RetryPolicy.fixed(3));
```

**Observed behavior**:
- The task runs once and then retries up to 3 additional attempts.
- If all attempts fail, the final error is propagated and the branch fails.
- Dependent tasks are not executed unless modeled as optional/fallback paths.


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
 
+ ---
+ 
+ ## 🚀 Full Sample Application
+ 
+ For a complete, runnable application demonstrating all these features together, check out our official **Basic Demo**:
+ 
+ [**FlowForge Samples on GitHub**](https://github.com/royada-labs/flowforge-samples/tree/main/spring-boot/basic-demo)
