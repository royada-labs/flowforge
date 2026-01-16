package io.tugrandsolutions.flowforge.workflow;

import io.tugrandsolutions.flowforge.task.BasicTask;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.input.DefaultTaskInputResolver;
import io.tugrandsolutions.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowPlanBuilder;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class ExactlyOnceConcurrencyTest {

    @Test
    void should_execute_each_task_exactly_once_under_parallel_scheduler() {
        var counts = new ConcurrentHashMap<TaskId, AtomicInteger>();

        io.tugrandsolutions.flowforge.task.TaskId A = new TaskId("A");
        TaskId B = new TaskId("B");
        TaskId C = new TaskId("C");
        TaskId D = new TaskId("D");

        List<Task<?, ?>> tasks = List.of(
                new CountingTask(A, Set.of(), counts, 50),
                new CountingTask(B, Set.of(A), counts, 50),
                new CountingTask(C, Set.of(A), counts, 50),
                new CountingTask(D, Set.of(B, C), counts, 50)
        );

        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(tasks);

        ReactiveWorkflowOrchestrator orchestrator =
                new ReactiveWorkflowOrchestrator(
                        Schedulers.parallel(),
                        new NoOpWorkflowMonitor(),
                        new DefaultTaskInputResolver()
                );

        StepVerifier.create(orchestrator.execute(plan, "x"))
                .assertNext(ctx -> {
                    assertEquals(1, counts.get(A).get(), "Task A executed more than once");
                    assertEquals(1, counts.get(B).get(), "Task B executed more than once");
                    assertEquals(1, counts.get(C).get(), "Task C executed more than once");
                    assertEquals(1, counts.get(D).get(), "Task D executed more than once");
                })
                .verifyComplete();
    }

    static final class CountingTask extends BasicTask<Object, Object> {

        private final Set<TaskId> deps;
        private final ConcurrentHashMap<TaskId, AtomicInteger> counts;
        private final long delayMs;

        CountingTask(TaskId id,
                     Set<TaskId> deps,
                     ConcurrentHashMap<TaskId, AtomicInteger> counts,
                     long delayMs) {
            super(id);
            this.deps = deps;
            this.counts = counts;
            this.delayMs = delayMs;
            this.counts.putIfAbsent(id, new AtomicInteger(0));
        }

        @Override
        public Set<TaskId> dependencies() {
            return deps;
        }

        @Override
        protected Mono<Object> doExecute(Object input, ReactiveExecutionContext context) {
            counts.get(id()).incrementAndGet();
            return Mono.delay(Duration.ofMillis(delayMs)).thenReturn(id().getValue());
        }
    }
}