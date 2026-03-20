package org.royada.flowforge.workflow.instance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.task.BasicTask;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;

class WorkflowInstanceTest {

    @Test
    void isFinished_should_return_false_when_only_one_task_completed_but_not_all() {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class, a)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        
        instance.tryMarkRunning(nodeA);
        instance.markCompleted(nodeA);
        
        // isFinished es false porque B aún no está en estado terminal
        assertFalse(instance.isFinished());
    }

    @Test
    void isFinished_should_return_true_when_all_tasks_completed() {
        TaskId a = TaskId.of("A");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        
        instance.tryMarkRunning(nodeA);
        instance.markCompleted(nodeA);
        
        assertTrue(instance.isFinished());
    }

    @Test
    void readyTasks_should_contain_dependent_after_dependency_completes() {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class, a)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        TaskNode nodeB = plan.getNode(b).orElseThrow();
        
        // A está listo al inicio
        assertEquals(1, instance.readyTasks().size());
        assertTrue(instance.readyTasks().contains(nodeA));
        
        // Completamos A
        instance.tryMarkRunning(nodeA);
        instance.markCompleted(nodeA);
        
        // Ahora B debería estar listo
        assertEquals(1, instance.readyTasks().size());
        assertTrue(instance.readyTasks().contains(nodeB));
    }

    @Test
    void readyTasks_should_be_empty_when_all_completed() {
        TaskId a = TaskId.of("A");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        
        instance.tryMarkRunning(nodeA);
        instance.markCompleted(nodeA);
        
        assertTrue(instance.readyTasks().isEmpty());
        assertTrue(instance.isFinished());
    }

    @Test
    void isFinished_should_return_true_with_failed_tasks() {
        TaskId a = TaskId.of("A");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        
        instance.tryMarkRunning(nodeA);
        instance.markFailed(nodeA);
        
        assertTrue(instance.isFinished());
    }

    @Test
    void markFailed_optional_should_not_cascade_failure() {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            taskOptional(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class, a)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        TaskNode nodeB = plan.getNode(b).orElseThrow();
        
        instance.tryMarkRunning(nodeA);
        instance.markSkipped(nodeA);
        
        instance.tryMarkRunning(nodeB);
        instance.markSkipped(nodeB);
        
        assertEquals(TaskStatus.SKIPPED, instance.status(nodeB));
    }

    @Test
    void failDownstream_should_not_affect_already_completed_tasks() {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        TaskNode nodeB = plan.getNode(b).orElseThrow();
        
        instance.tryMarkRunning(nodeA);
        instance.markCompleted(nodeA);
        
        instance.tryMarkRunning(nodeB);
        instance.markCompleted(nodeB);
        
        instance.markFailed(nodeA);
        
        assertEquals(TaskStatus.COMPLETED, instance.status(nodeB));
    }

    @Test
    void tryMarkRunning_should_return_false_if_not_ready() {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class, a)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeB = plan.getNode(b).orElseThrow();
        
        assertFalse(instance.tryMarkRunning(nodeB));
        assertEquals(TaskStatus.PENDING, instance.status(nodeB));
    }

    private ReactiveExecutionContext createContext() {
        return new org.royada.flowforge.workflow.InMemoryReactiveExecutionContext();
    }

    private Task<Void, Integer> task(TaskId id, Class<Void> inputType, Class<Integer> outputType, TaskId... dependencies) {
        return new BasicTask<Void, Integer>(id, inputType, outputType) {
            @Override
            public Set<TaskId> dependencies() {
                return Set.of(dependencies);
            }
            @Override
            protected Mono<Integer> doExecute(Void input, ReactiveExecutionContext ctx) {
                return Mono.just(1);
            }
        };
    }

    private Task<Void, Integer> taskOptional(TaskId id, Class<Void> inputType, Class<Integer> outputType, TaskId... dependencies) {
        return new BasicTask<Void, Integer>(id, inputType, outputType) {
            @Override
            public Set<TaskId> dependencies() {
                return Set.of(dependencies);
            }
            @Override
            public boolean optional() {
                return true;
            }
            @Override
            protected Mono<Integer> doExecute(Void input, ReactiveExecutionContext ctx) {
                return Mono.just(1);
            }
        };
    }
}
