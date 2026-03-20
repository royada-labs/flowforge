package org.royada.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.task.BasicTask;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.plan.InvalidPlanException;
import org.royada.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;

class WorkflowGraphValidationTest {

    private static final TaskId A = TaskId.of("A");
    private static final TaskId B = TaskId.of("B");

    @Test
    void should_fail_when_cycle_exists() {
        List<Task<?, ?>> tasks = List.of(
                new TaskA_DependsOnB(),
                new TaskB_DependsOnA());

        InvalidPlanException ex = assertThrows(
                InvalidPlanException.class,
                () -> WorkflowPlanBuilder.build(tasks));

        assertTrue(ex.getMessage().toLowerCase().contains("cycle"));
    }

    @Test
    void should_fail_when_dependency_is_missing() {
        TaskId missing = TaskId.of("MISSING");

        List<Task<?, ?>> tasks = List.of(
                new TaskWithMissingDependency(missing));

        InvalidPlanException ex = assertThrows(
                InvalidPlanException.class,
                () -> WorkflowPlanBuilder.build(tasks));

        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        assertTrue(msg.contains("missing") || msg.contains("not found"),
                "Expected error message to mention missing dependency, but was: " + ex.getMessage());
    }

    static final class TaskA_DependsOnB extends BasicTask<Object, String> {

        TaskA_DependsOnB() {
            super(A, Object.class, String.class);
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
            super(B, Object.class, String.class);
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
            super(TaskId.of("X"), Object.class, String.class);
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