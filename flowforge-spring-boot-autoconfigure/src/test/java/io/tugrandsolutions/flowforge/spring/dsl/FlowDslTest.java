package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlowDslTest {

    @Test
    void should_build_linear_plan() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(task("A"));
        reg.register(task("B"));
        reg.register(task("C"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        WorkflowExecutionPlan plan = dsl
                .start("A")
                .then("B")
                .then("C")
                .build();

        assertNotNull(plan);

        // These asserts depend on your core plan API.
        // If plan exposes nodes/roots, assert them here.
        // Minimal smoke: build returned non-null and no exception.
    }

    @Test
    void should_build_fork_join_plan() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(task("A"));
        reg.register(task("B"));
        reg.register(task("C"));
        reg.register(task("D"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        WorkflowExecutionPlan plan = dsl
                .start("A")
                .fork(
                        b -> b.then("B"),
                        b -> b.then("C")
                )
                .join("D")
                .build();

        assertNotNull(plan);
    }

    @Test
    void should_fail_on_unknown_task_id() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(task("A"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> dsl.start("A").then("X").build()
        );

        assertTrue(ex.getMessage().contains("Unknown task id: X"));
    }

    private static Task<Void, Object> task(String id) {
        return new Task<Void, Object>() {
            @Override public TaskId id() { return new TaskId(id); }
            @Override public Set<TaskId> dependencies() { return Set.of(); }
            @Override public boolean optional() { return false; }
            @Override public Mono<Object> execute(Void input, ReactiveExecutionContext ctx) {
                return Mono.just("ok");
            }
        };
    }
}