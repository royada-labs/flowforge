package org.royada.flowforge.spring.dsl;

import org.royada.flowforge.api.FlowForgeClient;
import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.annotations.TaskHandler;
import org.royada.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MethodReferenceOverloadIntegrationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                    .withUserConfiguration(OverloadConfig.class);

    @Test
    void should_resolve_overloaded_method_references_by_full_signature() {
        contextRunner.run(ctx -> {
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.executeResult("overload-int-flow", null))
                    .expectNext("int:7")
                    .verifyComplete();

            StepVerifier.create(client.executeResult("overload-long-flow", null))
                    .expectNext("long:9")
                    .verifyComplete();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class OverloadConfig {
        @Bean
        Ops ops() {
            return new Ops();
        }

        @Bean
        @FlowWorkflow(id = "overload-int-flow")
        WorkflowExecutionPlan intFlow(FlowDsl dsl) {
            TaskCallRef<Ops, Integer, String> convertInt = Ops::convert;
            return dsl.flow(Ops::startInt)
                    .then(convertInt)
                    .build();
        }

        @Bean
        @FlowWorkflow(id = "overload-long-flow")
        WorkflowExecutionPlan longFlow(FlowDsl dsl) {
            TaskCallRef<Ops, Long, String> convertLong = Ops::convert;
            return dsl.flow(Ops::startLong)
                    .then(convertLong)
                    .build();
        }
    }

    @TaskHandler("overload")
    static class Ops {
        @FlowTask(id = "startInt")
        public Mono<Integer> startInt(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(7);
        }

        @FlowTask(id = "startLong")
        public Mono<Long> startLong(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(9L);
        }

        @FlowTask(id = "convertInt")
        public Mono<String> convert(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("int:" + input);
        }

        @FlowTask(id = "convertLong")
        public Mono<String> convert(Long input, ReactiveExecutionContext ctx) {
            return Mono.just("long:" + input);
        }
    }
}
