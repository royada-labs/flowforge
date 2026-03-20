package org.royada.flowforge.spring.bootstrap;

import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import org.royada.flowforge.registry.WorkflowPlanRegistry;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
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
            return org.royada.flowforge.workflow.plan.WorkflowPlanBuilder.build(
                    java.util.List.of(
                            taskA,
                            (org.royada.flowforge.task.Task<?, ?>) taskB
                    )
            );
        }
    }

    static final class TaskA extends org.royada.flowforge.task.BasicTask<Void, Integer> {
        TaskA() {
            super(org.royada.flowforge.task.TaskId.of("A"), Void.class, Integer.class);
        }
        @Override protected reactor.core.publisher.Mono<Integer> doExecute(Void input, ReactiveExecutionContext ctx) {
            return reactor.core.publisher.Mono.just(1);
        }
    }


    static final class TaskB extends org.royada.flowforge.task.BasicTask<Integer, Integer> {
        TaskB() {
            super(org.royada.flowforge.task.TaskId.of("B"), Integer.class, Integer.class);
        }
        @Override public java.util.Set<org.royada.flowforge.task.TaskId> dependencies() {
            return java.util.Set.of(org.royada.flowforge.task.TaskId.of("A"));
        }
        @Override protected reactor.core.publisher.Mono<Integer> doExecute(Integer input, ReactiveExecutionContext ctx) {
            return reactor.core.publisher.Mono.just((input == null ? 0 : input) + 1);
        }
    }
}
