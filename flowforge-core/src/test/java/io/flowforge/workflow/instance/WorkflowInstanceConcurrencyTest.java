package io.flowforge.workflow.instance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.flowforge.task.BasicTask;
import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;

class WorkflowInstanceConcurrencyTest {

    @Test
    void isFinished_cache_should_be_thread_safe() throws InterruptedException {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        TaskId c = TaskId.of("C");
        TaskId d = TaskId.of("D");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class),
            task(c, Void.class, Integer.class),
            task(d, Void.class, Integer.class)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    TaskNode[] nodes = plan.nodes().toArray(TaskNode[]::new);
                    for (int j = 0; j < 100; j++) {
                        for (TaskNode node : nodes) {
                            if (instance.status(node) == io.flowforge.workflow.instance.TaskStatus.READY) {
                                instance.tryMarkRunning(node);
                                instance.markCompleted(node);
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        
        assertTrue(instance.isFinished());
    }

    @Test
    void concurrent_markCompleted_should_not_corrupt_status() throws InterruptedException {
        TaskId a = TaskId.of("A");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                instance.markCompleted(nodeA);
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        assertEquals(TaskStatus.COMPLETED, instance.status(nodeA));
    }

    @Test
    void concurrent_failDownstream_should_handle_correctly() throws InterruptedException {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        TaskId c = TaskId.of("C");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class, a),
            task(c, Void.class, Integer.class, a)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        TaskNode nodeA = plan.getNode(a).orElseThrow();
        
        instance.markCompleted(nodeA);
        
        CountDownLatch latch = new CountDownLatch(2);
        
        new Thread(() -> {
            instance.tryMarkRunning(plan.getNode(TaskId.of("B")).orElseThrow());
            instance.markFailed(plan.getNode(TaskId.of("B")).orElseThrow());
            latch.countDown();
        }).start();
        
        new Thread(() -> {
            instance.tryMarkRunning(plan.getNode(TaskId.of("C")).orElseThrow());
            instance.markCompleted(plan.getNode(TaskId.of("C")).orElseThrow());
            latch.countDown();
        }).start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        assertEquals(TaskStatus.COMPLETED, instance.status(nodeA));
    }

    @Test
    void readyTasks_should_be_thread_safe() throws InterruptedException {
        TaskId a = TaskId.of("A");
        TaskId b = TaskId.of("B");
        TaskId c = TaskId.of("C");
        
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
            task(a, Void.class, Integer.class),
            task(b, Void.class, Integer.class),
            task(c, Void.class, Integer.class, a, b)
        ));

        WorkflowInstance instance = new WorkflowInstance(plan, createContext());
        int iterations = 100;
        CountDownLatch latch = new CountDownLatch(iterations);

        for (int i = 0; i < iterations; i++) {
            new Thread(() -> {
                try {
                    instance.readyTasks();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private ReactiveExecutionContext createContext() {
        return new io.flowforge.workflow.InMemoryReactiveExecutionContext();
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
}
