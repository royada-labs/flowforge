package io.flowforge.spring.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LegacyWorkflowExecutionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
            .withUserConfiguration(LegacyConfig.class);

    @Test
    void should_execute_existing_bean_workflow_without_behavior_changes() {
        runner.run(ctx -> {
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.executeResult("legacy-flow", null))
                    .expectNext("value=41")
                    .verifyComplete();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class LegacyConfig {

        @Bean
        LegacyStartTask legacyStartTask() {
            return new LegacyStartTask();
        }

        @Bean
        LegacyFormatTask legacyFormatTask() {
            return new LegacyFormatTask();
        }

        @Bean
        @FlowWorkflow(id = "legacy-flow")
        WorkflowExecutionPlan legacyFlow(FlowDsl dsl) {
            return dsl.startTyped(TaskDefinition.of("legacy-start", Void.class, Integer.class))
                    .then(TaskDefinition.of("legacy-format", Integer.class, String.class))
                    .build();
        }
    }

    @FlowTask(id = "legacy-start")
    static class LegacyStartTask implements FlowTaskHandler<Void, Integer> {
        @Override
        public Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(40);
        }
    }

    @FlowTask(id = "legacy-format")
    static class LegacyFormatTask implements FlowTaskHandler<Integer, String> {
        @Override
        public Mono<String> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("value=" + (input + 1));
        }
    }
}
