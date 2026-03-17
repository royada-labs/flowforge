package io.flowforge.workflow;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.input.DefaultTaskInputResolver;
import io.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowE2ETest {

    private static final TaskId A = new TaskId("A");
    private static final TaskId B = new TaskId("B");
    private static final TaskId C = new TaskId("C");
    private static final TaskId D = new TaskId("D");

    @Test
    void should_execute_dag_and_propagate_outputs_via_context() {
        // A -> B
        // A -> C
        // B,C -> D
        List<Task<?, ?>> tasks = List.of(
                new TaskA(),
                new TaskB(),
                new TaskC(),
                new TaskD()
        );

        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);

        ReactiveWorkflowOrchestrator orchestrator =
                new ReactiveWorkflowOrchestrator(
                        Schedulers.immediate(),
                        new NoOpWorkflowMonitor(),
                        new DefaultTaskInputResolver()
                );

        StepVerifier.create(orchestrator.execute(plan, "hello"))
                .assertNext(ctx -> {
                    // A: length("hello") = 5
                    assertEquals(5, ctx.get(TaskDefinition.of(A, String.class, Integer.class).outputKey()).orElse(null));

                    // B: double(A) = 10
                    assertEquals(10, ctx.get(TaskDefinition.of(B, Integer.class, Integer.class).outputKey()).orElse(null));

                    // C: "len=5"
                    assertEquals("len=5", ctx.get(TaskDefinition.of(C, Integer.class, String.class).outputKey()).orElse(null));

                    // D: combine(B, C)
                    assertEquals("B=10;C=len=5", ctx.get(TaskDefinition.of(D, Map.class, String.class).outputKey()).orElse(null));
                })
                .verifyComplete();
    }

    /* ============================= */
    /* === Test Tasks (no Spring) === */
    /* ============================= */

    static final class TaskA extends BasicTask<String, Integer> {

        TaskA() {
            super(A);
        }

        @Override
        protected Mono<Integer> doExecute(String input, ReactiveExecutionContext context) {
            return Mono.just(input.length());
        }
    }

    static final class TaskB extends BasicTask<Integer, Integer> {

        TaskB() {
            super(B);
        }

        @Override
        public Set<TaskId> dependencies() {
            return Set.of(A);
        }

        @Override
        protected Mono<Integer> doExecute(Integer input, ReactiveExecutionContext context) {
            return Mono.just(input * 2);
        }
    }

    static final class TaskC extends BasicTask<Integer, String> {

        TaskC() {
            super(C);
        }

        @Override
        public Set<TaskId> dependencies() {
            return Set.of(A);
        }

        @Override
        protected Mono<String> doExecute(Integer input, ReactiveExecutionContext context) {
            return Mono.just("len=" + input);
        }
    }

    static final class TaskD extends BasicTask<Map<TaskId, Object>, String> {

        TaskD() {
            super(D);
        }

        @Override
        public Set<TaskId> dependencies() {
            return Set.of(B, C);
        }

        @Override
        protected Mono<String> doExecute(Map<TaskId, Object> input, ReactiveExecutionContext context) {
            Object bVal = input.get(B);
            Object cVal = input.get(C);

            assertNotNull(bVal, "Expected B input");
            assertNotNull(cVal, "Expected C input");

            return Mono.just("B=" + bVal + ";C=" + cVal);
        }
    }
}