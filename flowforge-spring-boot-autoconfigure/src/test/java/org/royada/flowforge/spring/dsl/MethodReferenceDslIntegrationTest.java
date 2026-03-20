package org.royada.flowforge.spring.dsl;

import org.royada.flowforge.api.FlowForgeClient;
import org.royada.flowforge.api.FlowTaskHandler;
import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import org.royada.flowforge.spring.registry.TaskDefinitionRegistry;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MethodReferenceDslIntegrationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                    .withUserConfiguration(MethodRefConfig.class);

    @Test
    void method_reference_dsl_should_execute_without_manual_taskdefinition() {
        contextRunner.run(ctx -> {
            assertNull(ctx.getStartupFailure());
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.executeResult("method-ref-flow", null))
                    .expectNext(3)
                    .verifyComplete();
        });
    }

    @Test
    void scanner_should_infer_types_from_task_methods_at_startup() {
        contextRunner.run(ctx -> {
            TaskDefinitionRegistry defs = ctx.getBean(TaskDefinitionRegistry.class);

            Class<?> producerOut = defs.find(TaskId.of("producer"))
                    .orElseThrow()
                    .outputType();
            Class<?> toStringIn = defs.find(TaskId.of("toStringTask"))
                    .orElseThrow()
                    .inputType();

            assertEquals(Integer.class, producerOut);
            assertEquals(Integer.class, toStringIn);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class MethodRefConfig {

        @Bean
        @FlowTask(id = "producer")
        FlowTaskHandler<Void, Integer> producer() {
            return (input, ctx) -> Mono.just(7);
        }

        @Bean
        @FlowTask(id = "toStringTask")
        FlowTaskHandler<Integer, String> toStringTask() {
            return (input, ctx) -> Mono.just("v=" + input);
        }

        @Bean
        @FlowTask(id = "lenTask")
        FlowTaskHandler<String, Integer> lenTask() {
            return (input, ctx) -> Mono.just(input.length());
        }

        @FlowWorkflow(id = "method-ref-flow")
        @Bean
        WorkflowExecutionPlan methodRefFlow(FlowDsl dsl) {
            return dsl.flow(MethodRefConfig::producer)
                    .then(MethodRefConfig::toStringTask)
                    .then(MethodRefConfig::lenTask)
                    .build();
        }
    }
}
