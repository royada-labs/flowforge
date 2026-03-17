package io.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskDescriptor;
import io.flowforge.task.TaskId;
import io.flowforge.task.FlowKey;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.instance.WorkflowInstance;
import io.flowforge.workflow.monitor.WorkflowMonitor;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import io.flowforge.workflow.policy.TimeoutPolicy;
import io.flowforge.workflow.report.ExecutionReport;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ProductionReadinessTest {

  @Test
  void point_A_verify_report_available_before_completion() {
    TaskId A = new TaskId("A");
    Task<?, ?> taskA = new BasicTask<Object, Object>(A) {
      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.just("A");
      }
    };

    AtomicBoolean reportReceivedBeforeComplete = new AtomicBoolean(false);

    WorkflowMonitor monitor = new WorkflowMonitor() {
      @Override
      public void onWorkflowComplete(WorkflowInstance instance, ExecutionReport report) {
        reportReceivedBeforeComplete.set(true);
      }
    };

    WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(taskA));
    ReactiveWorkflowOrchestrator orchestrator = new ReactiveWorkflowOrchestrator(
        reactor.core.scheduler.Schedulers.immediate(),
        monitor,
        new io.flowforge.workflow.input.DefaultTaskInputResolver());

    StepVerifier.create(orchestrator.execute(plan, null))
        .expectSubscription()
        .assertNext(ctx -> {
          assertTrue(reportReceivedBeforeComplete.get(),
              "Report must be received via monitor BEFORE the final context emission");
        })
        .verifyComplete();
  }

  @Test
  void point_C_verify_per_task_timeout_behavior() {
    // 1. Required task times out -> Workflow fails
    // 2. Optional task times out -> Workflow continues

    TaskId RequiredSlow = new TaskId("RequiredSlow");
    TaskId OptionalSlow = new TaskId("OptionalSlow");
    TaskId Dependent = new TaskId("Dependent"); // Depends on OptionalSlow

    // Timeout of 50ms
    TimeoutPolicy fastTimeout = TimeoutPolicy.of(Duration.ofMillis(50));

    Task<?, ?> tReqSlow = new BasicTask<Object, Object>(RequiredSlow) {
      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofMillis(200)).thenReturn("Slow");
      }
    };

    Task<?, ?> tOptSlow = new BasicTask<Object, Object>(OptionalSlow) {
      @Override
      public boolean optional() {
        return true;
      }

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.delay(Duration.ofMillis(200)).thenReturn("Slow");
      }
    };

    Task<?, ?> tDependent = new BasicTask<Object, Object>(Dependent) {
      @Override
      public java.util.Set<TaskId> dependencies() {
        return java.util.Set.of(OptionalSlow);
      }

      @Override
      protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
        return Mono.just("DependentRan");
      }
    };

    // Case 1: Required task times out
    TaskDescriptor dReq = new TaskDescriptor(tReqSlow, fastTimeout);
    WorkflowExecutionPlan plan1 = WorkflowExecutionPlan.from(
        io.flowforge.workflow.graph.WorkflowGraph.build(List.of(dReq)));

    StepVerifier.create(new ReactiveWorkflowOrchestrator().execute(plan1, null))
        .expectError()
        .verify();

    // Case 2: Optional task times out, Dependent runs
    TaskDescriptor dOpt = new TaskDescriptor(tOptSlow, fastTimeout);
    TaskDescriptor dDep = new TaskDescriptor(tDependent); // Use default policy

    WorkflowExecutionPlan plan2 = WorkflowExecutionPlan.from(
        io.flowforge.workflow.graph.WorkflowGraph.build(List.of(dOpt, dDep)));

        StepVerifier.create(new ReactiveWorkflowOrchestrator().execute(plan2, null))
            .assertNext(ctx -> {
              FlowKey<String> dependentKey = TaskDefinition.of(Dependent, Object.class, String.class).outputKey();
              assertEquals("DependentRan", ctx.get(dependentKey).orElse(null));
            })
        .verifyComplete();
  }
}
