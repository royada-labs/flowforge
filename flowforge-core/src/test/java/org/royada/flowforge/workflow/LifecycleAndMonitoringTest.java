package org.royada.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.task.BasicTask;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.input.DefaultTaskInputResolver;
import org.royada.flowforge.workflow.instance.WorkflowInstance;
import org.royada.flowforge.workflow.monitor.WorkflowMonitor;
import org.royada.flowforge.workflow.orchestrator.BackpressureStrategy;
import org.royada.flowforge.workflow.orchestrator.ExecutionLimits;
import org.royada.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class LifecycleAndMonitoringTest {

  @Test
  void cancellation_should_stop_execution() {
    // 5.2 Cancellation
    TaskId A = TaskId.of("A");
    AtomicBoolean aRan = new AtomicBoolean(false);
    AtomicBoolean aCancelled = new AtomicBoolean(false);

    Task<?, ?> taskA = new BasicTask<Object, Object>(A, Object.class, Object.class) {

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofMillis(500))
            .doOnSubscribe(s -> aRan.set(true))
            .doOnCancel(() -> aCancelled.set(true))
            .thenReturn("A");
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA));
    ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder().build();

    StepVerifier.create(orchestrator.execute(plan, null))
        .thenAwait(Duration.ofMillis(100)) // wait for start
        .thenCancel() // Cancel the subscription
        .verify();

    // Verify it started
    assertTrue(aRan.get(), "Task should have started");
    // Verify it was cancelled (scheduler dependent, but parallel/elastic usually
    // interrupt)
    // With Mono.delay, it handles cancellation correctly.
    // We wait a bit to ensure the cancel signal propagates
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }
    assertTrue(aCancelled.get(), "Task should have been cancelled");
  }

  @Test
  void monitor_events_should_be_ordered() {
    // 6.1 Event ordering
    TaskId A = TaskId.of("A");
    RecordingMonitor monitor = new RecordingMonitor();

    Task<?, ?> taskA = new BasicTask<Object, Object>(A, Object.class, Object.class) {

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.just("ok");
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA));
    ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder()
        .taskScheduler(Schedulers.immediate())
        .stateScheduler(Schedulers.newSingle("state"))
        .monitor(monitor)
        .inputResolver(new DefaultTaskInputResolver())
        .limits(new ExecutionLimits(2, 1000, BackpressureStrategy.BLOCK))
        .build();

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectNextCount(1)
        .verifyComplete();

    List<String> events = monitor.events;
    // Expect: WorkflowStart -> TaskStart(A) -> TaskSuccess(A) -> WorkflowComplete
    // Note: TaskStart and Success might be inverted if immediate scheduler is used
    // in a specific way,
    // but physically logic is Start before Execute.

    System.out.println(events);

    assertTrue(events.contains("WorkflowStart"), "Missing WorkflowStart");
    assertTrue(events.contains("TaskStart:A"), "Missing TaskStart:A");
    assertTrue(events.contains("TaskSuccess:A"), "Missing TaskSuccess:A");
    assertTrue(events.contains("WorkflowComplete"), "Missing WorkflowComplete");

    int startIdx = events.indexOf("TaskStart:A");
    int successIdx = events.indexOf("TaskSuccess:A");
    assertTrue(startIdx < successIdx, "TaskStart must precede TaskSuccess");
  }

  static class RecordingMonitor implements WorkflowMonitor {
    final List<String> events = new CopyOnWriteArrayList<>();

    @Override
    public void onWorkflowStart(WorkflowInstance instance) {
      events.add("WorkflowStart");
    }

    @Override
    public void onWorkflowComplete(WorkflowInstance instance) {
      events.add("WorkflowComplete");
    }

    @Override
    public void onTaskStart(WorkflowInstance instance, TaskId taskId) {
      events.add("TaskStart:" + taskId.getValue());
    }

    @Override
    public void onTaskSuccess(WorkflowInstance instance, TaskId taskId) {
      events.add("TaskSuccess:" + taskId.getValue());
    }

    @Override
    public void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error) {
      events.add("TaskFailure:" + taskId.getValue());
    }

    @Override
    public void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {
      events.add("TaskSkipped:" + taskId.getValue());
    }
  }
}
