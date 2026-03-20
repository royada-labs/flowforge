package org.royada.flowforge.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.exception.WorkflowConfigurationException;
import org.royada.flowforge.task.BasicTask;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;

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

        WorkflowConfigurationException ex = assertThrows(
                WorkflowConfigurationException.class,
                () -> registry.register(descriptor("duplicate-flow"))
        );

        assertTrue(ex.getMessage().contains("Duplicate workflow id"));
    }

    @Test
    void should_be_immutable_after_seal() {
        WorkflowRegistry registry = new WorkflowRegistry();
        registry.register(descriptor("flow-sealed"));
        registry.seal();

        WorkflowConfigurationException ex = assertThrows(
                WorkflowConfigurationException.class,
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
