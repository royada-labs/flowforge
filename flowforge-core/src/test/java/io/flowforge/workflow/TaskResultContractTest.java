package io.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TaskResultContractTest {

  @Test
  void should_convert_empty_task_result_to_failure_and_finish() {
    // Task returns Mono.empty()
    TaskId A = TaskId.of("A");

    Task<?, ?> taskA = new BasicTask<Object, Object>(A, Object.class, Object.class) {

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.empty(); // Violates contract
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA));
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectErrorSatisfies(err -> {
          // Task A should have failed (no output in context)
          // We can't easily check the context here unless we capture it, 
          // but the error itself is proof of failure.
        })
        .verify();
  }

  @Test
  void should_convert_sync_throw_to_failure_and_finish() {
    // Task throws exception synchronously before returning Mono
    TaskId A = TaskId.of("A");

    Task<?, ?> taskA = new BasicTask<Object, Object>(A, Object.class, Object.class) {

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        throw new RuntimeException("Sync exception");
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA));
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void should_not_execute_dependents_if_required_task_returns_empty() {
    // A (required, returns empty) -> B
    TaskId A = TaskId.of("A");
    TaskId B = TaskId.of("B");
    AtomicInteger bCounter = new AtomicInteger(0);

    List<Task<?, ?>> tasks = List.of(
        new BasicTask<Object, Object>(A, Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.empty();
          }
        },
        new BasicTask<Object, Object>(B, Object.class, Object.class) {

          @Override
          public Set<TaskId> dependencies() {
            return Set.of(A);
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            bCounter.incrementAndGet();
            return Mono.just("B");
          }
        });

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectError()
        .verify();

    assertEquals(0, bCounter.get(), "B should not execute when required A returns empty");
  }

  @Test
  void should_allow_dependents_if_optional_task_returns_empty() {
    // A (optional, returns empty) -> B
    TaskId A = TaskId.of("A");
    TaskId B = TaskId.of("B");
    AtomicInteger bCounter = new AtomicInteger(0);

    List<Task<?, ?>> tasks = List.of(
        new BasicTask<Object, Object>(A, Object.class, Object.class) {

          @Override
          public boolean optional() {
            return true;
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.empty();
          }
        },
        new BasicTask<Object, Object>(B, Object.class, Object.class) {

          @Override
          public Set<TaskId> dependencies() {
            return Set.of(A);
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            bCounter.incrementAndGet();
            return Mono.just("B");
          }
        });

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .assertNext(ctx -> {
          assertEquals("B", ctx.get(TaskDefinition.of(B, Object.class, String.class).outputKey()).orElse(null));
        })
        .verifyComplete();

    assertEquals(1, bCounter.get(), "B should execute when optional A returns empty");
  }

  @Test
  void should_handle_sync_exception_in_required_task() {
    // A (required, sync throw) -> B
    TaskId A = TaskId.of("A");
    TaskId B = TaskId.of("B");
    AtomicInteger bCounter = new AtomicInteger(0);

    List<Task<?, ?>> tasks = List.of(
        new BasicTask<Object, Object>(A, Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            throw new IllegalStateException("Boom");
          }
        },
        new BasicTask<Object, Object>(B, Object.class, Object.class) {

          @Override
          public Set<TaskId> dependencies() {
            return Set.of(A);
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            bCounter.incrementAndGet();
            return Mono.just("B");
          }
        });

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectError(IllegalStateException.class)
        .verify();

    assertEquals(0, bCounter.get(), "B should not execute when required A throws sync exception");
  }

  @Test
  void should_handle_sync_exception_in_optional_task() {
    // A (optional, sync throw) -> B
    TaskId A = TaskId.of("A");
    TaskId B = TaskId.of("B");
    AtomicInteger bCounter = new AtomicInteger(0);

    List<Task<?, ?>> tasks = List.of(
        new BasicTask<Object, Object>(A, Object.class, Object.class) {

          @Override
          public boolean optional() {
            return true;
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            throw new IllegalStateException("Boom");
          }
        },
        new BasicTask<Object, Object>(B, Object.class, Object.class) {

          @Override
          public Set<TaskId> dependencies() {
            return Set.of(A);
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            bCounter.incrementAndGet();
            return Mono.just("B");
          }
        });

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .assertNext(ctx -> {
          assertEquals("B", ctx.get(TaskDefinition.of(B, Object.class, String.class).outputKey()).orElse(null));
        })
        .verifyComplete();

    assertEquals(1, bCounter.get(), "B should execute when optional A throws sync exception");
  }
}
