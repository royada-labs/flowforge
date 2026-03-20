package org.royada.flowforge.workflow.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.workflow.graph.WorkflowGraph;
import org.royada.flowforge.workflow.input.DefaultTaskInputResolver;
import org.royada.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
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
      ReactiveWorkflowOrchestrator orchestrator = ReactiveWorkflowOrchestrator.builder()
          .taskScheduler(Schedulers.boundedElastic())
          .stateScheduler(sharedStateScheduler)
          .monitor(new NoOpWorkflowMonitor())
          .inputResolver(new DefaultTaskInputResolver())
          .limits(new ExecutionLimits(2, 1000, BackpressureStrategy.BLOCK))
          .build();

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
