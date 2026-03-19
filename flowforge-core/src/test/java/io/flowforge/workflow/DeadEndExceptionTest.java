package io.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.flowforge.task.BasicTask;
import io.flowforge.task.TaskId;
import io.flowforge.exception.DeadEndException;
import io.flowforge.workflow.input.DefaultTaskInputResolver;
import io.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class DeadEndExceptionTest {

  @Test
  void task_failure_should_fail_workflow_not_dead_end() {
    TaskId A = TaskId.of("A");
    TaskId B = TaskId.of("B");
    TaskId C = TaskId.of("C");

    // A fails
    // B depends on A. A is FAILED, so B is marked FAILED (immediate dependent).
    // C depends on B. B is FAILED. But logic only marks immediate dependents. So C
    // stays PENDING?
    // If C stays PENDING, then workflow is NOT finished.
    // And C cannot run because B is FAILED.
    // So DeadEndException occurs.

    List<io.flowforge.task.Task<?, ?>> tasks = List.of(
        new FailingTask(A),
        new PendingTask(B, Set.of(A)),
        new PendingTask(C, Set.of(B)));

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);

    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        Schedulers.parallel(),
        new NoOpWorkflowMonitor(),
        new DefaultTaskInputResolver());

    StepVerifier.create(orchestrator.execute(plan, "input"))
        .expectErrorSatisfies(err -> {
          if (err instanceof DeadEndException) {
            throw new AssertionError("Should not throw DeadEndException when a task explicitly failed", err);
          }
          assertEquals("Boom!", err.getMessage());
        })
        .verify();
  }

  static final class FailingTask extends BasicTask<Object, String> {
    FailingTask(TaskId id) {
      super(id, Object.class, String.class);
    }


    @Override
    protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
      return Mono.error(new RuntimeException("Boom!"));
    }
  }

  static final class PendingTask extends BasicTask<Object, String> {
    private final Set<TaskId> deps;

    PendingTask(TaskId id, Set<TaskId> deps) {
      super(id, Object.class, String.class);
      this.deps = deps;
    }


    @Override
    public Set<TaskId> dependencies() {
      return deps;
    }

    @Override
    protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
      return Mono.just("Ok");
    }
  }
}
