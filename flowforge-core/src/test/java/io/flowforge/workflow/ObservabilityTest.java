package io.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.instance.TaskStatus;
import io.flowforge.workflow.instance.WorkflowInstance;
import io.flowforge.workflow.monitor.WorkflowMonitor;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import io.flowforge.workflow.report.ExecutionReport;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ObservabilityTest {

  @Test
  void monitor_should_receive_one_terminal_event_per_task() {
    // DAG with success, skipped, and failure
    TaskId A = TaskId.of("A");
    TaskId B = TaskId.of("B"); // optional, will fail
    TaskId C = TaskId.of("C"); // depends on B, should run
    TaskId D = TaskId.of("D"); // required, will fail
    TaskId E = TaskId.of("E"); // depends on D, should not run

    List<Task<?, ?>> tasks = List.of(
        new BasicTask<Object, Object>(A, Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("A");
          }
        },
        new BasicTask<Object, Object>(B, Object.class, Object.class) {

          @Override
          public boolean optional() {
            return true;
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.error(new RuntimeException("B fails"));
          }
        },
        new BasicTask<Object, Object>(C, Object.class, Object.class) {

          @Override
          public Set<TaskId> dependencies() {
            return Set.of(B);
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("C");
          }
        },
        new BasicTask<Object, Object>(D, Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.error(new RuntimeException("D fails"));
          }
        },
        new BasicTask<Object, Object>(E, Object.class, Object.class) {

          @Override
          public Set<TaskId> dependencies() {
            return Set.of(D);
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("E");
          }
        });

    ConcurrentHashMap<TaskId, Integer> startCount = new ConcurrentHashMap<>();
    ConcurrentHashMap<TaskId, Integer> terminalCount = new ConcurrentHashMap<>();

    WorkflowMonitor monitor = new WorkflowMonitor() {
      @Override
      public void onTaskStart(WorkflowInstance instance, TaskId taskId) {
        startCount.merge(taskId, 1, Integer::sum);
      }

      @Override
      public void onTaskSuccess(WorkflowInstance instance, TaskId taskId, Duration duration) {
        terminalCount.merge(taskId, 1, Integer::sum);
      }

      @Override
      public void onTaskSkipped(WorkflowInstance instance, TaskId taskId, Duration duration) {
        terminalCount.merge(taskId, 1, Integer::sum);
      }

      @Override
      public void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error, Duration duration) {
        terminalCount.merge(taskId, 1, Integer::sum);
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        reactor.core.scheduler.Schedulers.immediate(),
        monitor,
        new io.flowforge.workflow.input.DefaultTaskInputResolver());

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectError()
        .verify();

    // A: success, B: skipped (optional failure), C: success, D: failed, E: failed
    // (cascade)
    assertEquals(1, startCount.get(A), "A should start once");
    assertEquals(1, terminalCount.get(A), "A should have exactly one terminal event");

    assertEquals(1, startCount.get(B), "B should start once");
    assertEquals(1, terminalCount.get(B), "B should have exactly one terminal event (skipped)");

    assertEquals(1, startCount.get(C), "C should start once");
    assertEquals(1, terminalCount.get(C), "C should have exactly one terminal event");

    assertEquals(1, startCount.get(D), "D should start once");
    assertEquals(1, terminalCount.get(D), "D should have exactly one terminal event (failed)");

    // E should not start because D failed
    assertNull(startCount.get(E), "E should not start");
    assertNull(terminalCount.get(E), "E should not have terminal event");
  }

  @Test
  void monitor_should_capture_durations_non_negative() {
    TaskId A = TaskId.of("A");
    AtomicReference<Duration> capturedDuration = new AtomicReference<>();

    Task<?, ?> taskA = new BasicTask<Object, Object>(A, Object.class, Object.class) {

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofMillis(50)).thenReturn("A");
      }
    };

    WorkflowMonitor monitor = new WorkflowMonitor() {
      @Override
      public void onTaskSuccess(WorkflowInstance instance, TaskId taskId, Duration duration) {
        if (taskId.equals(A)) {
          capturedDuration.set(duration);
        }
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA));
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        reactor.core.scheduler.Schedulers.boundedElastic(),
        monitor,
        new io.flowforge.workflow.input.DefaultTaskInputResolver());

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectNextCount(1)
        .verifyComplete();

    assertNotNull(capturedDuration.get(), "Duration should be captured");
    assertTrue(capturedDuration.get().toMillis() >= 40,
        "Duration should be at least ~40ms (allowing some variance)");
    assertTrue(capturedDuration.get().toMillis() < 500,
        "Duration should be less than 500ms (sanity check)");
  }

  @Test
  void report_should_reflect_final_statuses_and_counts() {
    TaskId A = TaskId.of("A");
    TaskId B = TaskId.of("B"); // optional, fails
    TaskId C = TaskId.of("C"); // depends on B
    TaskId D = TaskId.of("D"); // fails

    List<Task<?, ?>> tasks = List.of(
        new BasicTask<Object, Object>(A, Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("A");
          }
        },
        new BasicTask<Object, Object>(B, Object.class, Object.class) {

          @Override
          public boolean optional() {
            return true;
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.error(new RuntimeException("B fails"));
          }
        },
        new BasicTask<Object, Object>(C, Object.class, Object.class) {

          @Override
          public Set<TaskId> dependencies() {
            return Set.of(B);
          }

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("C");
          }
        },
        new BasicTask<Object, Object>(D, Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.error(new RuntimeException("D fails"));
          }
        });

    AtomicReference<ExecutionReport> capturedReport = new AtomicReference<>();

    WorkflowMonitor monitor = new WorkflowMonitor() {
      @Override
      public void onWorkflowStart(WorkflowInstance instance) {
      }

      @Override
      public void onTaskStart(WorkflowInstance instance, TaskId taskId) {
      }

      @Override
      public void onTaskSuccess(WorkflowInstance instance, TaskId taskId) {
      }

      @Override
      public void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {
      }

      @Override
      public void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error) {
      }

      @Override
      public void onWorkflowComplete(WorkflowInstance instance, ExecutionReport report) {
        capturedReport.set(report);
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        reactor.core.scheduler.Schedulers.immediate(),
        monitor,
        new io.flowforge.workflow.input.DefaultTaskInputResolver());

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectError()
        .verify();

    ExecutionReport report = capturedReport.get();
    assertNotNull(report, "Report should be captured");

    assertEquals(4, report.getTotalTasks(), "Total tasks should be 4");
    assertEquals(2, report.getCompletedTasks(), "Completed tasks should be 2 (A, C)");
    assertEquals(1, report.getSkippedTasks(), "Skipped tasks should be 1 (B)");
    assertEquals(1, report.getFailedTasks(), "Failed tasks should be 1 (D)");

    assertEquals(TaskStatus.COMPLETED, report.getFinalStatuses().get(A));
    assertEquals(TaskStatus.SKIPPED, report.getFinalStatuses().get(B));
    assertEquals(TaskStatus.COMPLETED, report.getFinalStatuses().get(C));
    assertEquals(TaskStatus.FAILED, report.getFinalStatuses().get(D));

    assertTrue(report.getDuration(A).isPresent(), "A should have duration");
    assertTrue(report.getDuration(B).isPresent(), "B should have duration");
    assertTrue(report.getDuration(C).isPresent(), "C should have duration");
    assertTrue(report.getDuration(D).isPresent(), "D should have duration");

    assertTrue(report.getError(D).isPresent(), "D should have error");
    assertEquals("D fails", report.getError(D).get().getMessage());
  }
}
