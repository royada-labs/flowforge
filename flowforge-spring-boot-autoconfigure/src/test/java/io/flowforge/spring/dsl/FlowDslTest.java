package io.flowforge.spring.dsl;

import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.spring.registry.TaskProvider;
import io.flowforge.spring.registry.TaskDefinitionRegistry;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlowDslTest {

    @Test
    void should_build_linear_plan() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        TaskDefinitionRegistry defs = new TaskDefinitionRegistry();
        reg.register(provider("A"));
        reg.register(provider("B"));
        reg.register(provider("C"));

        FlowDsl dsl = new DefaultFlowDsl(reg, defs);

        WorkflowExecutionPlan plan = dsl
                .startTyped(TaskDefinition.of("A", Void.class, Object.class))
                .then(TaskDefinition.of("B", Object.class, Object.class))
                .then(TaskDefinition.of("C", Object.class, Object.class))
                .build();


        assertNotNull(plan);
    }

    @Test
    void should_build_fork_join_plan() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        TaskDefinitionRegistry defs = new TaskDefinitionRegistry();
        reg.register(provider("A"));
        reg.register(provider("B"));
        reg.register(provider("C"));
        reg.register(provider("D"));

        FlowDsl dsl = new DefaultFlowDsl(reg, defs);

        WorkflowExecutionPlan plan = dsl
                .startTyped(TaskDefinition.of("A", Void.class, Object.class))
                .fork(
                        b -> b.then(TaskDefinition.of("B", Object.class, Object.class)),
                        b -> b.then(TaskDefinition.of("C", Object.class, Object.class))
                )
                .join(TaskDefinition.of("D", Object.class, Object.class))
                .build();



        assertNotNull(plan);
    }

    @Test
    void should_fail_on_unknown_task_id() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        TaskDefinitionRegistry defs = new TaskDefinitionRegistry();
        reg.register(provider("A"));

        FlowDsl dsl = new DefaultFlowDsl(reg, defs);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> dsl.startTyped(TaskDefinition.of("A", Void.class, Object.class))
                         .then(TaskDefinition.of("X", Object.class, Object.class))
                         .build()
        );


        assertTrue(ex.getMessage().contains("Unknown task id: X"));
    }

    private static TaskProvider provider(String id) {
        TaskId taskId = TaskId.of(id);
        return new TaskProvider() {
            @Override public TaskId id() { return taskId; }

            @Override
            public Task<Void, Object> get() {
                return task(id);
            }
        };
    }

    private static Task<Void, Object> task(String id) {
        return new Task<>() {
            @Override public TaskId id() { return TaskId.of(id); }
            @Override public Set<TaskId> dependencies() { return Set.of(); }
            @Override public boolean optional() { return false; }
            @Override public Class<Void> inputType() { return Void.class; }
            @Override public Class<Object> outputType() { return Object.class; }
            @Override public Mono<Object> execute(Void input, ReactiveExecutionContext ctx) {
                return Mono.just("ok");
            }
        };
    }
}
