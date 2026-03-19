package io.flowforge.spring.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.annotations.TaskHandler;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.spring.registry.TaskDefinitionRegistry;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AnnotatedTaskHandlerIntegrationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                    .withUserConfiguration(AnnotatedHandlerConfig.class);

        private final ApplicationContextRunner noContextRunner =
            new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                .withUserConfiguration(AnnotatedHandlerNoContextConfig.class);

    @Test
    void should_register_and_execute_taskhandler_methods_via_method_references() {
        contextRunner.run(ctx -> {
            assertNull(ctx.getStartupFailure());
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.executeResult("annotated-handler-flow", null))
                    .expectNext("branches=2")
                    .verifyComplete();
        });
    }

    @Test
    void should_register_task_metadata_for_taskhandler_methods() {
        contextRunner.run(ctx -> {
            TaskDefinitionRegistry defs = ctx.getBean(TaskDefinitionRegistry.class);
            assertEquals(Integer.class, defs.find(TaskId.of("startWork")).orElseThrow().outputType());
            assertEquals(Object.class, defs.find(TaskId.of("aggregate")).orElseThrow().inputType());
        });
    }

    @Test
    void should_accept_method_references_without_context_parameter() {
        noContextRunner.run(ctx -> {
            assertNull(ctx.getStartupFailure());
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.executeResult("annotated-noctx-flow", null))
                    .expectNext("v=11")
                    .verifyComplete();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedHandlerConfig {
        @Bean
        Ops ops() {
            return new Ops();
        }

        @FlowWorkflow(id = "annotated-handler-flow")
        @Bean
        WorkflowExecutionPlan plan(FlowDsl dsl) {
            return dsl.flow(Ops::startWork)
                    .parallel(Ops::profileLookup, Ops::ordersLookup)
                    .join(Ops::aggregate)
                    .then(Ops::finalizeResult)
                    .build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedHandlerNoContextConfig {
        @Bean
        OpsNoContext opsNoContext() {
            return new OpsNoContext();
        }

        @FlowWorkflow(id = "annotated-noctx-flow")
        @Bean
        WorkflowExecutionPlan planNoContext(FlowDsl dsl) {
            return dsl.flow(OpsNoContext::start)
                    .then(OpsNoContext::format)
                    .build();
        }
    }

    @TaskHandler("customer")
    static class Ops {
        @FlowTask(id = "startWork")
        public Mono<Integer> startWork(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(7);
        }

        @FlowTask(id = "profileLookup")
        public Mono<String> profileLookup(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("u" + input);
        }

        @FlowTask(id = "ordersLookup")
        public Mono<String> ordersLookup(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("o" + (input * 2));
        }

        @FlowTask(id = "aggregate")
        public Mono<Integer> aggregate(Object input, ReactiveExecutionContext ctx) {
            if (input instanceof Map<?, ?> m) {
                return Mono.just(m.size());
            }
            return Mono.just(0);
        }

        @FlowTask(id = "finalizeResult")
        public Mono<String> finalizeResult(Integer branches, ReactiveExecutionContext ctx) {
            return Mono.just("branches=" + branches);
        }
    }

    @TaskHandler("customer-noctx")
    static class OpsNoContext {
        @FlowTask(id = "start")
        public Mono<Integer> start(Void input) {
            return Mono.just(11);
        }

        @FlowTask(id = "format")
        public Mono<String> format(Integer input) {
            return Mono.just("v=" + input);
        }
    }
}
