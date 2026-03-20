package org.royada.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.task.BasicTask;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskDescriptor;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.workflow.input.DefaultTaskInputResolver;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
import org.royada.flowforge.workflow.monitor.WorkflowMonitor;
import org.royada.flowforge.workflow.orchestrator.BackpressureStrategy;
import org.royada.flowforge.workflow.orchestrator.ExecutionLimits;
import org.royada.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.policy.ExecutionPolicy;
import org.royada.flowforge.workflow.policy.RetryPolicy;
import org.royada.flowforge.workflow.policy.TimeoutPolicy;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class PolicyIntegrationTest {

  @Test
  void should_retry_task_on_failure() {
    // 7.1 Retries
    TaskId A = TaskId.of("A");
    AtomicInteger attempts = new AtomicInteger(0);

    Task<?, ?> taskA = new BasicTask<Object, Object>(A, Object.class, Object.class) {

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
        org.royada.flowforge.workflow.graph.WorkflowGraph.build(List.of(descriptor)));

    ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder().build();

    StepVerifier.create(orchestrator.execute(plan, null))
        .assertNext(ctx -> {
          assertEquals("Success", ctx.get(TaskDefinition.of(A, Object.class, String.class).outputKey()).orElse(null));
        })
        .verifyComplete();

    assertEquals(3, attempts.get(), "Should have attempted 3 times (2 fails + 1 success)");
  }

  @Test
  void should_fail_on_timeout_verified_by_monitor() {
    TaskId A = TaskId.of("A");
    AtomicInteger failureCount = new AtomicInteger(0);

    Task<?, ?> taskA = new BasicTask<Object, Object>(A, Object.class, Object.class) {

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofMillis(500)).thenReturn("Too slow");
      }
    };

    ExecutionPolicy policy = TimeoutPolicy.of(Duration.ofMillis(100));
    TaskDescriptor descriptor = new TaskDescriptor(taskA, policy);

    WorkflowExecutionPlan plan = WorkflowExecutionPlan.from(
        org.royada.flowforge.workflow.graph.WorkflowGraph.build(List.of(descriptor)));

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

    ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder()
        .taskScheduler(Schedulers.parallel())
        .stateScheduler(Schedulers.newSingle("state"))
        .monitor(monitor)
        .inputResolver(new DefaultTaskInputResolver())
        .limits(new ExecutionLimits(2, 1000, BackpressureStrategy.BLOCK))
        .build();

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectError()
        .verify();

    assertEquals(1, failureCount.get(), "Task should have failed with TimeoutException");
  }
}
