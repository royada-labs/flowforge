package io.tugrandsolutions.flowforge.workflow;

import io.tugrandsolutions.flowforge.task.BasicTask;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowGraphValidationTest {

    private static final TaskId A = new TaskId("A");
    private static final TaskId B = new TaskId("B");

    @Test
    void should_fail_when_cycle_exists() {
        List<Task<?, ?>> tasks = List.of(
                new TaskA_DependsOnB(),
                new TaskB_DependsOnA()
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> WorkflowPlanBuilder.build(tasks)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("cycle"));
    }

    @Test
    void should_fail_when_dependency_is_missing() {
        TaskId missing = new TaskId("MISSING");

        List<Task<?, ?>> tasks = List.of(
                new TaskWithMissingDependency(missing)
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> WorkflowPlanBuilder.build(tasks)
        );

        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        assertTrue(msg.contains("missing") || msg.contains("not found"),
                "Expected error message to mention missing dependency, but was: " + ex.getMessage());
    }

    static final class TaskA_DependsOnB extends BasicTask<Object, String> {

        TaskA_DependsOnB() {
            super(A);
        }

        @Override
        public Set<TaskId> dependencies() {
            return Set.of(B);
        }

        @Override
        protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("A");
        }
    }

    static final class TaskB_DependsOnA extends BasicTask<Object, String> {

        TaskB_DependsOnA() {
            super(B);
        }

        @Override
        public Set<TaskId> dependencies() {
            return Set.of(A);
        }

        @Override
        protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("B");
        }
    }

    static final class TaskWithMissingDependency extends BasicTask<Object, String> {

        private final TaskId missing;

        TaskWithMissingDependency(TaskId missing) {
            super(new TaskId("X"));
            this.missing = missing;
        }

        @Override
        public Set<TaskId> dependencies() {
            return Set.of(missing);
        }

        @Override
        protected Mono<String> doExecute(Object input, ReactiveExecutionContext context) {
            return Mono.just("X");
        }
    }
}