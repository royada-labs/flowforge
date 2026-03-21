# Testing: Unit & Integration Workflows

Since FlowForge is built on Project Reactor, your workflows are naturally testable using standards like **`StepVerifier`**. You should test your workflows both in isolation and with Spring-level integration.

---

## 🧪 Unit Testing (Isolated DSL)

You can verify your `WorkflowDefinition` without starting a full Spring Application Context. This is the fastest way to test complex branching logic.

### Using `StepVerifier`
When you execute a workflow, you get a `Mono<ReactiveExecutionContext>`. This makes it easy to assert results:

```java
@Test
void task_sequence_should_propagate_data() {
    ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder().build();
    WorkflowExecutionPlan plan = ... // your DSL-built plan
    
    StepVerifier.create(orchestrator.execute(plan, "test-input"))
        .assertNext(ctx -> {
            // ✅ Assert that results exist in the context
            assertTrue(ctx.isCompleted(FlowKey.of("fetchTask", String.class)));
            
            // ✅ Assert the actual data produced
            assertEquals("processed", ctx.get("transformTask", String.class).orElse(null));
        })
        .verifyComplete();
}
```

---

## 🛠️ Mocking Tasks

Sometimes you don't want to execute a slow or external task (e.g. Payment Gateway) during tests. 

### Strategy 1: Spring Boot Test Mocking
In your `@SpringBootTest`, use `@MockBean` from Spring Test to mock the task handler components:

```java
@SpringBootTest
class WorkflowIntegrationTest {

    @MockBean
    private PaymentTasks paymentTasks; // Your @TaskHandler class

    @Test
    void payment_failure_should_trigger_rollback() {
        // 🎯 Stub the task to fail immediately
        when(paymentTasks.charge(any()))
            .thenReturn(Mono.error(new PaymentDeniedException()));

        StepVerifier.create(client.execute("order-process", new Order()))
            .expectError(PaymentDeniedException.class)
            .verify();
    }
}
```

---

## 🧩 Key Principles for Quality Tests

1.  **Test for Failures**: Don't just test the "Happy Path". Use `.fork()` and `.timeout()` to verify how the workflow reacts to delays.
2.  **Verify the Context State**: Sometimes a workflow finishes successfully but missed a non-critical optional task. Use `ctx.isCompleted()` to verify these "silent" details.
3.  **Trace Verification**: Use `client.executeWithTrace()` in tests to assert that tasks were actually executed in parallel (by checking overlapping timestamps).
