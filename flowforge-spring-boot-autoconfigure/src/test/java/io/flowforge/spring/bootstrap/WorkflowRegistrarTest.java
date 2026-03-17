package io.flowforge.spring.bootstrap;

import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.registry.WorkflowPlanRegistry;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowRegistrarTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(ImportAutoConfig.class);

    @Test
    void should_register_workflow_plan_from_annotated_bean() {
        runner.withUserConfiguration(ValidWorkflowConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    WorkflowPlanRegistry registry = ctx.getBean(WorkflowPlanRegistry.class);

                    assertTrue(registry.contains("api-score"));
                    WorkflowExecutionPlan plan = registry.find("api-score").orElseThrow();

                    // sanity checks: el plan no es null y tiene nodos
                    assertFalse(plan.nodes().isEmpty());
                    assertEquals(1, plan.roots().size());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(FlowForgeAutoConfiguration.class)
    static class ImportAutoConfig { }

    @Configuration(proxyBeanMethods = false)
    static class ValidWorkflowConfig {

        @Bean TaskA taskA() { return new TaskA(); }
        @Bean TaskB taskB() { return new TaskB(); }

        @Bean
        @FlowWorkflow(id = "api-score")
        WorkflowExecutionPlan apiScoreWorkflow(TaskA taskA, TaskB taskB) {
            return io.flowforge.workflow.plan.WorkflowPlanBuilder.build(
                    java.util.List.of(
                            taskA,
                            (io.flowforge.task.Task<?, ?>) taskB
                    )
            );
        }
    }

    static final class TaskA extends io.flowforge.task.BasicTask<Void, Integer> {
        TaskA() {
            super(io.flowforge.task.TaskId.of("A"), Void.class, Integer.class);
        }
        @Override protected reactor.core.publisher.Mono<Integer> doExecute(Void input, ReactiveExecutionContext ctx) {
            return reactor.core.publisher.Mono.just(1);
        }
    }


    static final class TaskB extends io.flowforge.task.BasicTask<Integer, Integer> {
        TaskB() {
            super(io.flowforge.task.TaskId.of("B"), Integer.class, Integer.class);
        }
        @Override public java.util.Set<io.flowforge.task.TaskId> dependencies() {
            return java.util.Set.of(io.flowforge.task.TaskId.of("A"));
        }
        @Override protected reactor.core.publisher.Mono<Integer> doExecute(Integer input, ReactiveExecutionContext ctx) {
            return reactor.core.publisher.Mono.just((input == null ? 0 : input) + 1);
        }
    }
}
