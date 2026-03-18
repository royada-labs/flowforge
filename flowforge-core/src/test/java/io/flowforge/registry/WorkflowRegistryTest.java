package io.flowforge.registry;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowRegistryTest {

    @Test
    void should_register_and_retrieve_descriptors() {
        WorkflowRegistry registry = new WorkflowRegistry();
        WorkflowDescriptor descriptorA = descriptor("flow-a");
        WorkflowDescriptor descriptorB = descriptor("flow-b");

        registry.register(descriptorA);
        registry.register(descriptorB);

        assertTrue(registry.contains("flow-a"));
        assertTrue(registry.contains("flow-b"));
        assertEquals(2, registry.all().size());
        assertNotNull(registry.get("flow-a").plan());
    }

    @Test
    void should_fail_on_duplicate_workflow_id() {
        WorkflowRegistry registry = new WorkflowRegistry();
        registry.register(descriptor("duplicate-flow"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> registry.register(descriptor("duplicate-flow"))
        );

        assertTrue(ex.getMessage().contains("Duplicate workflow id"));
    }

    @Test
    void should_be_immutable_after_seal() {
        WorkflowRegistry registry = new WorkflowRegistry();
        registry.register(descriptor("flow-sealed"));
        registry.seal();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> registry.register(descriptor("late-flow"))
        );

        assertTrue(ex.getMessage().contains("immutable"));
    }

    private static WorkflowDescriptor descriptor(String id) {
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(task(id + "-task")));
        return new WorkflowDescriptor() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public WorkflowExecutionPlan plan() {
                return plan;
            }

            @Override
            public Class<?> source() {
                return WorkflowRegistryTest.class;
            }
        };
    }

    private static Task<Void, Integer> task(String id) {
        return new BasicTask<>(TaskId.of(id), Void.class, Integer.class) {
            @Override
            protected Mono<Integer> doExecute(Void input, ReactiveExecutionContext ctx) {
                return Mono.just(1);
            }
        };
    }
}
