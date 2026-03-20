package org.royada.flowforge.registry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.exception.UnknownWorkflowException;
import org.royada.flowforge.exception.WorkflowConfigurationException;
import org.royada.flowforge.task.BasicTask;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;

class WorkflowRegistryEdgeCasesTest {

    @Test
    void should_reject_workflow_with_null_id() {
        WorkflowRegistry registry = new WorkflowRegistry();
        
        WorkflowDescriptor descriptor = new WorkflowDescriptor() {
            @Override
            public String id() { return null; }
            @Override
            public WorkflowExecutionPlan plan() { return createPlan("A"); }
            @Override
            public Class<?> source() { return getClass(); }
        };

        WorkflowConfigurationException ex = assertThrows(
            WorkflowConfigurationException.class,
            () -> registry.register(descriptor)
        );

        assertTrue(ex.getMessage().contains("blank"));
    }

    @Test
    void should_reject_workflow_with_blank_id() {
        WorkflowRegistry registry = new WorkflowRegistry();
        
        WorkflowDescriptor descriptor = new WorkflowDescriptor() {
            @Override
            public String id() { return "   "; }
            @Override
            public WorkflowExecutionPlan plan() { return createPlan("A"); }
            @Override
            public Class<?> source() { return getClass(); }
        };

        WorkflowConfigurationException ex = assertThrows(
            WorkflowConfigurationException.class,
            () -> registry.register(descriptor)
        );

        assertTrue(ex.getMessage().contains("blank"));
    }

    @Test
    void should_reject_workflow_with_null_plan() {
        WorkflowRegistry registry = new WorkflowRegistry();
        
        WorkflowDescriptor descriptor = new WorkflowDescriptor() {
            @Override
            public String id() { return "test-flow"; }
            @Override
            public WorkflowExecutionPlan plan() { return null; }
            @Override
            public Class<?> source() { return getClass(); }
        };

        WorkflowConfigurationException ex = assertThrows(
            WorkflowConfigurationException.class,
            () -> registry.register(descriptor)
        );

        assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    void should_reject_null_descriptor() {
        WorkflowRegistry registry = new WorkflowRegistry();

        WorkflowConfigurationException ex = assertThrows(
            WorkflowConfigurationException.class,
            () -> registry.register(null)
        );

        assertTrue(ex.getMessage().contains("must not be null"));
    }

    @Test
    void get_should_throw_unknown_workflow_exception() {
        WorkflowRegistry registry = new WorkflowRegistry();

        UnknownWorkflowException ex = assertThrows(
            UnknownWorkflowException.class,
            () -> registry.get("non-existent")
        );

        assertTrue(ex.getMessage().contains("non-existent"));
    }

    @Test
    void contains_should_return_false_for_non_existent() {
        WorkflowRegistry registry = new WorkflowRegistry();

        assertFalse(registry.contains("non-existent"));
    }

    @Test
    void all_should_return_empty_for_empty_registry() {
        WorkflowRegistry registry = new WorkflowRegistry();

        assertTrue(registry.all().isEmpty());
    }

    @Test
    void snapshot_should_return_unmodifiable_collection() {
        WorkflowRegistry registry = new WorkflowRegistry();
        registry.register(createDescriptor("flow1"));
        registry.register(createDescriptor("flow2"));

        var snapshot = registry.snapshot();

        assertThrows(UnsupportedOperationException.class, () -> {
            snapshot.clear();
        });
    }

    @Test
    void seal_should_prevent_further_registration() {
        WorkflowRegistry registry = new WorkflowRegistry();
        registry.register(createDescriptor("flow1"));
        registry.seal();

        WorkflowConfigurationException ex = assertThrows(
            WorkflowConfigurationException.class,
            () -> registry.register(createDescriptor("flow2"))
        );

        assertTrue(ex.getMessage().contains("immutable"));
    }

    @Test
    void all_should_return_unmodifiable_collection() {
        WorkflowRegistry registry = new WorkflowRegistry();
        registry.register(createDescriptor("flow1"));

        var all = registry.all();

        assertThrows(UnsupportedOperationException.class, () -> {
            all.clear();
        });
    }

    @Test
    void duplicate_workflow_should_show_both_sources() {
        WorkflowRegistry registry = new WorkflowRegistry();
        
        registry.register(new WorkflowDescriptor() {
            @Override public String id() { return "dup-flow"; }
            @Override public WorkflowExecutionPlan plan() { return createPlan("A"); }
            @Override public Class<?> source() { return ClassA.class; }
        });

        WorkflowConfigurationException ex = assertThrows(
            WorkflowConfigurationException.class,
            () -> registry.register(new WorkflowDescriptor() {
                @Override public String id() { return "dup-flow"; }
                @Override public WorkflowExecutionPlan plan() { return createPlan("B"); }
                @Override public Class<?> source() { return ClassB.class; }
            })
        );

        assertTrue(ex.getMessage().contains("ClassA"));
        assertTrue(ex.getMessage().contains("ClassB"));
    }

    private WorkflowExecutionPlan createPlan(String... taskIds) {
        List<Task<Void, Integer>> tasks = new ArrayList<>();
        for (String id : taskIds) {
            TaskId taskId = TaskId.of(id);
            tasks.add(new BasicTask<Void, Integer>(taskId, Void.class, Integer.class) {
                @Override
                protected Mono<Integer> doExecute(Void input, ReactiveExecutionContext ctx) {
                    return Mono.just(1);
                }
            });
        }
        return WorkflowPlanBuilder.build(tasks);
    }

    private WorkflowDescriptor createDescriptor(String id) {
        return new WorkflowDescriptor() {
            @Override public String id() { return id; }
            @Override public WorkflowExecutionPlan plan() { return createPlan("A"); }
            @Override public Class<?> source() { return getClass(); }
        };
    }

    static class ClassA {}
    static class ClassB {}
}
