package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.spring.annotations.FlowWorkflow;
import io.tugrandsolutions.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.tugrandsolutions.flowforge.registry.WorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
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
            return io.tugrandsolutions.flowforge.workflow.plan.WorkflowPlanBuilder.build(
                    java.util.List.of(
                            taskA,
                            (io.tugrandsolutions.flowforge.task.Task<?, ?>) taskB
                    )
            );
        }
    }

    static final class TaskA implements io.tugrandsolutions.flowforge.task.Task<Void, Integer> {
        @Override public io.tugrandsolutions.flowforge.task.TaskId id() {
            return new io.tugrandsolutions.flowforge.task.TaskId("A");
        }
        @Override public reactor.core.publisher.Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
            return reactor.core.publisher.Mono.just(1);
        }
    }

    static final class TaskB implements io.tugrandsolutions.flowforge.task.Task<Integer, Integer> {
        @Override public io.tugrandsolutions.flowforge.task.TaskId id() {
            return new io.tugrandsolutions.flowforge.task.TaskId("B");
        }
        @Override public java.util.Set<io.tugrandsolutions.flowforge.task.TaskId> dependencies() {
            return java.util.Set.of(new io.tugrandsolutions.flowforge.task.TaskId("A"));
        }
        @Override public reactor.core.publisher.Mono<Integer> execute(Integer input, ReactiveExecutionContext ctx) {
            return reactor.core.publisher.Mono.just((input == null ? 0 : input) + 1);
        }
    }
}
