package io.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskDescriptor;
import io.flowforge.task.TaskId;
import io.flowforge.task.FlowKey;
import io.flowforge.workflow.input.DefaultTaskInputResolver;
import io.flowforge.workflow.instance.WorkflowInstance;
import io.flowforge.workflow.monitor.WorkflowMonitor;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.policy.ExecutionPolicy;
import io.flowforge.workflow.policy.RetryPolicy;
import io.flowforge.workflow.policy.TimeoutPolicy;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class PolicyIntegrationTest {

  @Test
  void should_retry_task_on_failure() {
    // 7.1 Retries
    TaskId A = new TaskId("A");
    AtomicInteger attempts = new AtomicInteger(0);

    Task<?, ?> taskA = new BasicTask<Object, Object>(A) {
      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        int count = attempts.incrementAndGet();
        if (count <= 2) {
          return Mono.error(new RuntimeException("Transient fail"));
        }
        return Mono.just("Success");
      }
    };

    // Retry 3 times
    ExecutionPolicy policy = RetryPolicy.fixed(3);
    TaskDescriptor descriptor = new TaskDescriptor(taskA, policy);

    WorkflowExecutionPlan plan = WorkflowExecutionPlan.from(
        io.flowforge.workflow.graph.WorkflowGraph.build(List.of(descriptor)));

    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    StepVerifier.create(orchestrator.execute(plan, null))
        .assertNext(ctx -> {
          assertEquals("Success", ctx.get(FlowKey.of(A, String.class)).orElse(null));
        })
        .verifyComplete();

    assertEquals(3, attempts.get(), "Should have attempted 3 times (2 fails + 1 success)");
  }

  @Test
  void should_fail_on_timeout_verified_by_monitor() {
    TaskId A = new TaskId("A");
    AtomicInteger failureCount = new AtomicInteger(0);

    Task<?, ?> taskA = new BasicTask<Object, Object>(A) {
      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofMillis(500)).thenReturn("Too slow");
      }
    };

    ExecutionPolicy policy = TimeoutPolicy.of(Duration.ofMillis(100));
    TaskDescriptor descriptor = new TaskDescriptor(taskA, policy);

    WorkflowExecutionPlan plan = WorkflowExecutionPlan.from(
        io.flowforge.workflow.graph.WorkflowGraph.build(List.of(descriptor)));

    WorkflowMonitor monitor = new WorkflowMonitor() {
      @Override
      public void onWorkflowStart(WorkflowInstance instance) {
      }

      @Override
      public void onWorkflowComplete(WorkflowInstance instance) {
      }

      @Override
      public void onTaskStart(WorkflowInstance instance, TaskId taskId) {
      }

      @Override
      public void onTaskSuccess(WorkflowInstance instance, TaskId taskId) {
      }

      @Override
      public void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error) {
        if (taskId.equals(A) && error instanceof TimeoutException) {
          failureCount.incrementAndGet();
        }
      }

      @Override
      public void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {
      }
    };

    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        Schedulers.parallel(),
        Schedulers.newSingle("state"),
        monitor,
        new DefaultTaskInputResolver(),
        2);

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectError()
        .verify();

    assertEquals(1, failureCount.get(), "Task should have failed with TimeoutException");
  }
}
