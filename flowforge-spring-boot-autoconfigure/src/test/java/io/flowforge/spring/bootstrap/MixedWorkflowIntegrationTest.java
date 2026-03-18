package io.flowforge.spring.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.registry.WorkflowRegistry;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.spring.workflow.WorkflowDefinition;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MixedWorkflowIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
            .withUserConfiguration(MixedConfig.class);

    @Test
    void should_register_and_execute_bean_and_class_based_workflows() {
        runner.run(ctx -> {
            WorkflowRegistry registry = ctx.getBean(WorkflowRegistry.class);
            assertEquals(2, registry.all().size());
            assertTrue(registry.contains("legacy-flow"));
            assertTrue(registry.contains("new-flow"));

            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.executeResult("legacy-flow", null))
                    .expectNext("legacy-11")
                    .verifyComplete();

            StepVerifier.create(client.executeResult("new-flow", null))
                    .expectNext("new-11")
                    .verifyComplete();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class MixedConfig {

        @Bean
        StartTask startTask() {
            return new StartTask();
        }

        @Bean
        LegacyLabelTask legacyLabelTask() {
            return new LegacyLabelTask();
        }

        @Bean
        NewLabelTask newLabelTask() {
            return new NewLabelTask();
        }

        @Bean
        @FlowWorkflow(id = "legacy-flow")
        WorkflowExecutionPlan legacyFlow(FlowDsl dsl) {
            return dsl.startTyped(TaskDefinition.of("start", Void.class, Integer.class))
                    .then(TaskDefinition.of("legacy-label", Integer.class, String.class))
                    .build();
        }

        @Bean
        NewFlow newFlow() {
            return new NewFlow();
        }
    }

    @FlowWorkflow(id = "new-flow")
    static class NewFlow implements WorkflowDefinition {
        @Override
        public WorkflowExecutionPlan define(FlowDsl dsl) {
            return dsl.startTyped(TaskDefinition.of("start", Void.class, Integer.class))
                    .then(TaskDefinition.of("new-label", Integer.class, String.class))
                    .build();
        }
    }

    @FlowTask(id = "start")
    static class StartTask implements FlowTaskHandler<Void, Integer> {
        @Override
        public Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(10);
        }
    }

    @FlowTask(id = "legacy-label")
    static class LegacyLabelTask implements FlowTaskHandler<Integer, String> {
        @Override
        public Mono<String> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("legacy-" + (input + 1));
        }
    }

    @FlowTask(id = "new-label")
    static class NewLabelTask implements FlowTaskHandler<Integer, String> {
        @Override
        public Mono<String> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("new-" + (input + 1));
        }
    }
}
