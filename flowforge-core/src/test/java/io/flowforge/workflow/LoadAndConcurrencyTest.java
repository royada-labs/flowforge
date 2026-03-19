package io.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.input.DefaultTaskInputResolver;
import io.flowforge.workflow.instance.WorkflowInstance;
import io.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.flowforge.workflow.monitor.WorkflowMonitor;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class LoadAndConcurrencyTest {

  @Test
  void should_not_starve_with_many_tasks_and_low_concurrency() {
    // 4.2 No starvation
    // 40 tasks, maxConcurrency = 4
    int taskCount = 40;
    int maxConcurrency = 4;

    AtomicInteger completedCount = new AtomicInteger(0);

    List<Task<?, ?>> tasks = IntStream.range(0, taskCount)
        .mapToObj(i -> new BasicTask<Object, Object>(TaskId.of("T" + i), Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.delay(Duration.ofMillis(50))
                .doOnNext(x -> completedCount.incrementAndGet())
                .thenReturn("done");
          }
        })
        .collect(Collectors.toList());

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        Schedulers.parallel(),
        Schedulers.newSingle("state"),
        new NoOpWorkflowMonitor(),
        new DefaultTaskInputResolver(),
        maxConcurrency);

    StepVerifier.create(orchestrator.execute(plan, null)
            .timeout(Duration.ofSeconds(30)))
        .expectNextCount(1)
        .verifyComplete();

    assertTrue(completedCount.get() == taskCount, "All tasks should have completed: " + completedCount.get() + "/" + taskCount);
  }

  @Test
  void should_respect_bounded_concurrency() {
    // 4.3 Bounded concurrency
    int taskCount = 20;
    int maxConcurrency = 3;

    // Active executions counter
    AtomicInteger activeExecutions = new AtomicInteger(0);
    AtomicInteger maxObserved = new AtomicInteger(0);

    List<Task<?, ?>> tasks = IntStream.range(0, taskCount)
        .mapToObj(i -> new BasicTask<Object, Object>(TaskId.of("T" + i), Object.class, Object.class) {

          @Override
          protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.defer(() -> {
              int current = activeExecutions.incrementAndGet();
              maxObserved.accumulateAndGet(current, Math::max);

              // Sleep to hold the slot
              return Mono.delay(Duration.ofMillis(50))
                  .doOnTerminate(() -> activeExecutions.decrementAndGet())
                  .doOnCancel(() -> activeExecutions.decrementAndGet())
                  .thenReturn("done");
            });
          }
        })
        .collect(Collectors.toList());

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        Schedulers.parallel(),
        Schedulers.newSingle("state"),
        new NoOpWorkflowMonitor(),
        new DefaultTaskInputResolver(),
        maxConcurrency);

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectNextCount(1)
        .verifyComplete();

    if (maxObserved.get() > maxConcurrency) {
      fail("Observed internal concurrency " + maxObserved.get() + " exceeded limit " + maxConcurrency);
    }
  }

  // Monitor class removed as it's no longer needed for this test
  static class ConcurrencyMonitor implements WorkflowMonitor {
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
    }

    @Override
    public void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {
    }
  }
}
