# Real-World Examples

The following examples demonstrate how to use FlowForge in production scenarios.

## 1. User Onboarding Flow

**Context**: When a user signs up, we need to create their account, send a welcome email, and provision a free tier resource.

```java
@Configuration
public class OnboardingFlow {

    @FlowWorkflow(id = "user-onboarding")
    @Bean
    public WorkflowExecutionPlan plan(FlowDsl dsl) {
        return dsl.startTyped(CREATE_ACCOUNT)
                  .fork(
                      branch -> branch.then(SEND_WELCOME_EMAIL),
                      branch -> branch.then(PROVISION_RESOURCE)
                  )
                  .join(NOTIFY_SLACK_ADMIN)
                  .build();
    }
}
```

**Behavior**: 
- `CREATE_ACCOUNT` runs first. 
- Once finished, `SEND_WELCOME_EMAIL` and `PROVISION_RESOURCE` run in parallel.
- `NOTIFY_SLACK_ADMIN` only runs after both parallel branches succeed.

---

## 2. API Orchestration (Typed)

**Context**: A gateway service that aggregates data from two microservices (User and Order) and calculates a discount.

```java
public class TypedOrchestration {
    
    static final TaskDefinition<Void, Object> START = TaskDefinition.of("start", Void.class, Object.class);
    static final TaskDefinition<Object, User> GET_USER = TaskDefinition.of("getUser", Object.class, User.class);
    static final TaskDefinition<Object, Order> GET_ORDER = TaskDefinition.of("getOrder", Object.class, Order.class);
    static final TaskDefinition<Object, Double> CALC_DISCOUNT = TaskDefinition.of("calc", Object.class, Double.class);

    @FlowWorkflow(id = "price-calculator")
    @Bean
    public WorkflowExecutionPlan plan(FlowDsl dsl) {
        return dsl.startTyped(START)
                  .fork(
                      b -> b.then(GET_USER),
                      b -> b.then(GET_ORDER)
                  )
                  .join(CALC_DISCOUNT)
                  .build();
    }
}
```

**Behavior**: 
`GET_USER` and `GET_ORDER` run in parallel after `START`. `CALC_DISCOUNT` runs when both branches complete.

Method-reference equivalent with `@TaskHandler`:

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
