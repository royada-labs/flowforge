package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.spring.registry.TaskProvider;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;


import static org.junit.jupiter.api.Assertions.*;

class FlowDslTest {

    @Test
    void should_build_linear_plan() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(provider("A"));
        reg.register(provider("B"));
        reg.register(provider("C"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        WorkflowExecutionPlan plan = dsl
                .start("A")
                .then("B")
                .then("C")
                .build();

        assertNotNull(plan);
    }

    @Test
    void should_build_fork_join_plan() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(provider("A"));
        reg.register(provider("B"));
        reg.register(provider("C"));
        reg.register(provider("D"));

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
        reg.register(provider("A"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> dsl.start("A").then("X").build()
        );

        assertTrue(ex.getMessage().contains("Unknown task id: X"));
    }

    private static TaskProvider<Void, Object> provider(String id) {
        TaskId taskId = new TaskId(id);
        return new TaskProvider<>() {
            @Override public TaskId id() { return taskId; }

            @Override
            public Task<Void, Object> get() {
                return task(id);
            }
        };
    }

    private static Task<Void, Object> task(String id) {
        return new Task<Void, Object>() {
            @Override public TaskId id() { return new TaskId(id); }
            @Override public Mono<Object> execute(Void input, ReactiveExecutionContext ctx) {
                return Mono.just("ok");
            }
        };
    }
}