package io.tugrandsolutions.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import io.tugrandsolutions.flowforge.task.BasicTask;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.input.DefaultTaskInputResolver;
import io.tugrandsolutions.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class SemanticsTest {

  @Test
  void topological_execution_order_should_be_respected() {
    // 1.1 Topological execution order
    // A (slow) -> C
    // B (fast) -> C
    // C must wait for both
    TaskId A = new TaskId("A");
    TaskId B = new TaskId("B");
    TaskId C = new TaskId("C");

    List<String> executionLog = new CopyOnWriteArrayList<>();

    List<Task<?, ?>> tasks = List.of(
        new RunnableTask(A, Set.of(), () -> {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
          }
          executionLog.add("A");
        }),
        new RunnableTask(B, Set.of(), () -> {
          executionLog.add("B");
        }),
        new RunnableTask(C, Set.of(A, B), () -> {
          executionLog.add("C");
        }));

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        Schedulers.parallel(), new NoOpWorkflowMonitor(), new DefaultTaskInputResolver());

    StepVerifier.create(orchestrator.execute(plan, "input"))
        .expectNextCount(1)
        .verifyComplete();

    // C must be last
    assertEquals("C", executionLog.get(2), "C should be executed last");
    assertTrue(executionLog.contains("A"));
    assertTrue(executionLog.contains("B"));
  }

  @Test
  void multiple_roots_should_execute() {
    // 1.2 Multi-root
    TaskId A = new TaskId("A");
    TaskId B = new TaskId("B");

    List<String> executionLog = new CopyOnWriteArrayList<>();

    List<Task<?, ?>> tasks = List.of(
        new RunnableTask(A, Set.of(), () -> executionLog.add("A")),
        new RunnableTask(B, Set.of(), () -> executionLog.add("B")));

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectNextCount(1)
        .verifyComplete();

    assertTrue(executionLog.contains("A"));
    assertTrue(executionLog.contains("B"));
    assertEquals(2, executionLog.size());
  }

  @Test
  void type_mismatch_should_fail_gracefully() {
    // 2.3 Type safety
    TaskId A = new TaskId("A"); // produces String "10"
    TaskId B = new TaskId("B"); // expects Integer

    List<Task<?, ?>> tasks = List.of(
        new BasicTask<Object, String>(A) {
          @Override
          protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("10");
          }
        },
        new BasicTask<Integer, String>(B) {
          @Override
          public Set<TaskId> dependencies() {
            return Set.of(A);
          }

          @Override
          protected Mono<String> doExecute(Integer input, ReactiveExecutionContext context) {
            return Mono.just("ok");
          }
        });

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .assertNext(ctx -> {
          // A succeeded
          assertEquals("10", ctx.get(A, String.class).orElse(null));
          // B failed due to class cast
          assertFalse(ctx.get(B, String.class).isPresent());
          // We verify B is effectively failed in the graph (implicit in the result
          // absence or logic)
          // Note: Orchestrator catches the cancellation/error.
          // To verify the error specifically we'd need a monitor or check the status map
          // which isn't public in context.
          // But effectively, context for B should be empty.
        })
        .verifyComplete();
  }

  @Test
  void mixed_dependencies_should_handle_optional_failure_and_required_success() {
    // 3.3 Mixed dependencies
    // A (required) -> C
    // B (optional, fails) -> C
    // C should run
    TaskId A = new TaskId("A");
    TaskId B = new TaskId("B");
    TaskId C = new TaskId("C");

    List<Task<?, ?>> tasks = List.of(
        new RunnableTask(A, Set.of(), () -> {
        }),
        new FailingTask(B, Set.of(), true), // optional failure
        new BasicTask<Object, String>(C) {
          @Override
          public Set<TaskId> dependencies() {
            return Set.of(A, B);
          }

          @Override
          protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("C executed");
          }
        });

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .assertNext(ctx -> {
          assertEquals("C executed", ctx.get(C, String.class).orElse(null));
        })
        .verifyComplete();
  }

  @Test
  void mixed_dependencies_should_block_if_required_fails() {
    // 3.3 Mixed dependencies
    // A (required, fails) -> C
    // B (optional) -> C
    // C should NOT run
    TaskId A = new TaskId("A");
    TaskId B = new TaskId("B");
    TaskId C = new TaskId("C");

    List<Task<?, ?>> tasks = List.of(
        new FailingTask(A, Set.of(), false), // required failure
        new RunnableTask(B, Set.of(), () -> {
        }),
        new BasicTask<Object, String>(C) {
          @Override
          public Set<TaskId> dependencies() {
            return Set.of(A, B);
          }

          @Override
          protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("Should not run");
          }
        });

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .assertNext(ctx -> {
          assertFalse(ctx.get(C, String.class).isPresent(), "C should not execute if required A fails");
        })
        .verifyComplete();
  }

  // --- Helpers ---

  static class RunnableTask extends BasicTask<Object, Object> {
    private final Set<TaskId> deps;
    private final Runnable action;

    RunnableTask(TaskId id, Set<TaskId> deps, Runnable action) {
      super(id);
      this.deps = deps;
      this.action = action;
    }

    @Override
    public Set<TaskId> dependencies() {
      return deps;
    }

    @Override
    protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
      return Mono.fromRunnable(action).thenReturn("ok");
    }
  }

  static class FailingTask extends BasicTask<Object, Object> {
    private final Set<TaskId> deps;
    private final boolean optional;

    FailingTask(TaskId id, Set<TaskId> deps, boolean optional) {
      super(id);
      this.deps = deps;
      this.optional = optional;
    }

    @Override
    public Set<TaskId> dependencies() {
      return deps;
    }

    @Override
    public boolean optional() {
      return optional;
    }

    @Override
    protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
      return Mono.error(new RuntimeException("Fail"));
    }
  }
}
