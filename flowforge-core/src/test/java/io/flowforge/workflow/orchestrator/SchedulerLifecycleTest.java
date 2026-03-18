package io.flowforge.workflow.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.flowforge.workflow.graph.WorkflowGraph;
import io.flowforge.workflow.input.DefaultTaskInputResolver;
import io.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class SchedulerLifecycleTest {

  @Test
  void shouldNotDisposeSharedStateSchedulerBetweenExecutions() {
    // 1. Create a shared scheduler
    Scheduler sharedStateScheduler = Schedulers.newSingle("shared-state");

    try {
      // 2. Create Orchestrator with the shared scheduler
      ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
          Schedulers.boundedElastic(),
          sharedStateScheduler,
          new NoOpWorkflowMonitor(),
          new DefaultTaskInputResolver(),
          2);

      // 3. Define a trivial plan (empty is fine)
      WorkflowExecutionPlan emptyPlan = WorkflowExecutionPlan.from(
          WorkflowGraph.build(Collections.emptyList()));

      // 4. First Execution
      StepVerifier.create(orchestrator.execute("run-1", emptyPlan, null))
          .expectNextCount(1)
          .verifyComplete();

      // 5. Verify Scheduler is still alive
      assertThat(sharedStateScheduler.isDisposed()).isFalse();

      // 6. Second Execution (would fail with RejectedExecutionException if scheduler
      // was disposed)
      StepVerifier.create(orchestrator.execute("run-2", emptyPlan, null))
          .expectNextCount(1)
          .verifyComplete();

    } finally {
      sharedStateScheduler.dispose();
    }
  }
}
