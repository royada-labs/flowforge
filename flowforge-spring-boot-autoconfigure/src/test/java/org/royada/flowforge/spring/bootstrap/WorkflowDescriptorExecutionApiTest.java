package org.royada.flowforge.spring.bootstrap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.royada.flowforge.api.FlowForgeClient;
import org.royada.flowforge.api.FlowTaskHandler;
import org.royada.flowforge.exception.UnknownWorkflowException;
import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import org.royada.flowforge.spring.dsl.FlowDsl;
import org.royada.flowforge.spring.workflow.WorkflowDefinition;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class WorkflowDescriptorExecutionApiTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
            .withUserConfiguration(ApiConfig.class);

    @Test
    void should_execute_by_workflow_id() {
        runner.run(ctx -> {
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);
            StepVerifier.create(client.execute("typed-flow", 21))
                    .assertNext(value -> assertNotNull(value))
                    .verifyComplete();

            StepVerifier.create(client.executeResult("typed-flow", 21))
                    .expectNext(42)
                    .verifyComplete();
        });
    }

    @Test
    void should_fail_for_invalid_workflow_id() {
        runner.run(ctx -> {
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);
            UnknownWorkflowException ex = assertThrows(
                    UnknownWorkflowException.class,
                    () -> client.execute("missing-id", 1)
            );
            assertTrue(ex.getMessage().contains("missing-id"));
        });
    }

    @Test
    void should_fail_for_invalid_input_type() {
        runner.run(ctx -> {
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> client.execute("typed-flow", "wrong-input")
            );
            assertTrue(ex.getMessage().contains("expects initial input compatible"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ApiConfig {

        @Bean
        MultiplyRootTask multiplyRootTask() {
            return new MultiplyRootTask();
        }

        @Bean
        TypedFlow typedFlow() {
            return new TypedFlow();
        }
    }

    @FlowWorkflow(id = "typed-flow")
    static class TypedFlow implements WorkflowDefinition {
        @Override
        public WorkflowExecutionPlan define(FlowDsl dsl) {
            return dsl.startTyped(TaskDefinition.of("multiply", Integer.class, Integer.class))
                    .build();
        }
    }

    @FlowTask(id = "multiply")
    static class MultiplyRootTask implements FlowTaskHandler<Integer, Integer> {
        @Override
        public Mono<Integer> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just(input * 2);
        }
    }
}
