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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OptionalFailurePropagationTest {

    @Test
    void optional_task_failure_should_be_skipped_and_dependents_should_run() {
        TaskId A = TaskId.of("A");
        TaskId B = TaskId.of("B"); // optional fails
        TaskId C = TaskId.of("C"); // depends on B, should still run

        AtomicInteger cRuns = new AtomicInteger(0);

        List<Task<?, ?>> tasks = List.of(
                new OkTask(A),
                new OptionalFailTask(B, Set.of(A)),
                new DependentTask(C, Set.of(B), cRuns)
        );

        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);

        ReactiveWorkflowOrchestrator orchestrator =
                new ReactiveWorkflowOrchestrator(
                        Schedulers.parallel(),
                        new NoOpWorkflowMonitor(),
                        new DefaultTaskInputResolver()
                );

        StepVerifier.create(orchestrator.execute(plan, "input"))
                .assertNext(ctx -> {
                    // A produce output
                    assertEquals("ok", ctx.get(TaskDefinition.of(A, Object.class, String.class).outputKey()).orElse(null));

                    // B falla pero es optional: NO garantizamos output en contexto
                    // (depende de tu BasicTask si guarda skipped o no). Lo importante:
                    assertEquals(1, cRuns.get(), "Dependent C should have run despite optional failure in B");

                    // C produce output
                    assertEquals("c", ctx.get(TaskDefinition.of(C, Object.class, String.class).outputKey()).orElse(null));
                })
                .verifyComplete();
    }

    @Test
    void non_optional_task_failure_should_block_dependents() {
        TaskId A = TaskId.of("A");
        TaskId B = TaskId.of("B"); // non-optional fails
        TaskId C = TaskId.of("C"); // depends on B, must NOT run

        AtomicInteger cRuns = new AtomicInteger(0);

        List<Task<?, ?>> tasks = List.of(
                new OkTask(A),
                new RequiredFailTask(B, Set.of(A)),
                new DependentTask(C, Set.of(B), cRuns)
        );

        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);

        ReactiveWorkflowOrchestrator orchestrator =
                new ReactiveWorkflowOrchestrator(
                        Schedulers.parallel(),
                        new NoOpWorkflowMonitor(),
                        new DefaultTaskInputResolver()
                );

        StepVerifier.create(orchestrator.execute(plan, "input"))
                .expectError()
                .verify();
    }

    /* ============================= */
    /* === Test Tasks (no Spring) === */
    /* ============================= */

    static final class OkTask extends BasicTask<Object, String> {
        OkTask(TaskId id) { super(id, Object.class, String.class); }


        @Override
        protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("ok");
        }
    }

    static final class OptionalFailTask extends BasicTask<Object, String> {

        private final Set<TaskId> deps;

        OptionalFailTask(TaskId id, Set<TaskId> deps) {
            super(id, Object.class, String.class);
            this.deps = deps;
        }


        @Override
        public Set<TaskId> dependencies() {
            return deps;
        }

        @Override
        public boolean optional() {
            return true;
        }

        @Override
        protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.error(new RuntimeException("boom (optional)"));
        }
    }

    static final class RequiredFailTask extends BasicTask<Object, String> {

        private final Set<TaskId> deps;

        RequiredFailTask(TaskId id, Set<TaskId> deps) {
            super(id, Object.class, String.class);
            this.deps = deps;
        }


        @Override
        public Set<TaskId> dependencies() {
            return deps;
        }

        @Override
        public boolean optional() {
            return false;
        }

        @Override
        protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.error(new RuntimeException("boom (required)"));
        }
    }

    static final class DependentTask extends BasicTask<Object, String> {

        private final Set<TaskId> deps;
        private final AtomicInteger runs;

        DependentTask(TaskId id, Set<TaskId> deps, AtomicInteger runs) {
            super(id, Object.class, String.class);
            this.deps = deps;
            this.runs = runs;
        }


        @Override
        public Set<TaskId> dependencies() {
            return deps;
        }

        @Override
        protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            runs.incrementAndGet();
            return Mono.just("c");
        }
    }
}
