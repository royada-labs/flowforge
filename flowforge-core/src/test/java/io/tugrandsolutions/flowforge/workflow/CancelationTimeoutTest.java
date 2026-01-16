package io.tugrandsolutions.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.tugrandsolutions.flowforge.task.BasicTask;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.instance.TaskStatus;
import io.tugrandsolutions.flowforge.workflow.instance.WorkflowInstance;
import io.tugrandsolutions.flowforge.workflow.monitor.WorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowPlanBuilder;
import io.tugrandsolutions.flowforge.workflow.report.ExecutionReport;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CancelationTimeoutTest {

  @Test
  void should_stop_execution_on_cancellation() {
    TaskId A = new TaskId("A");
    TaskId B = new TaskId("B");

    // Task A takes a long time
    Task<?, ?> taskA = new BasicTask<Object, Object>(A) {
      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofSeconds(5)).thenReturn("A");
      }
    };

    // Task B depends on A, should never run
    Task<?, ?> taskB = new BasicTask<Object, Object>(B) {
      @Override
      public java.util.Set<TaskId> dependencies() {
        return java.util.Set.of(A);
      }

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.just("B");
      }
    };

    AtomicBoolean cancelled = new AtomicBoolean(false);
    AtomicReference<ExecutionReport> capturedReport = new AtomicReference<>();

    WorkflowMonitor monitor = new WorkflowMonitor() {
      @Override
      public void onWorkflowComplete(WorkflowInstance instance, ExecutionReport report) {
        cancelled.set(true);
        capturedReport.set(report);
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA, taskB));
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        reactor.core.scheduler.Schedulers.parallel(),
        monitor,
        new io.tugrandsolutions.flowforge.workflow.input.DefaultTaskInputResolver());

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectSubscription()
        .thenAwait(Duration.ofMillis(100))
        .thenCancel() // Trigger cancellation
        .verify();

    assertTrue(cancelled.get(), "Workflow monitor should be notified of completion (cancellation)");
    assertNotNull(capturedReport.get(), "Report should be generated on cancellation");

    // Report assertions
    ExecutionReport report = capturedReport.get();
    assertEquals(0, report.getCompletedTasks(), "No tasks should be completed");
    // Status of A might be RUNNING or SCHEDULED depending on race, but definitely
    // not COMPLETED
    assertNotEquals(TaskStatus.COMPLETED, report.getFinalStatuses().get(A));
    assertNotEquals(TaskStatus.COMPLETED, report.getFinalStatuses().get(B));
  }

  @Test
  void should_support_convenience_timeout_overload() {
    TaskId A = new TaskId("A");

    Task<?, ?> taskA = new BasicTask<Object, Object>(A) {
      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofSeconds(5)).thenReturn("A");
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA));
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator();

    // Expect TimeoutException
    StepVerifier.create(orchestrator.execute(plan, null, Duration.ofMillis(100)))
        .expectError(java.util.concurrent.TimeoutException.class)
        .verify();
  }
}
