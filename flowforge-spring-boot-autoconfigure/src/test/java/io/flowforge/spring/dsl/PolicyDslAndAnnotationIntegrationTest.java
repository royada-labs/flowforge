package io.flowforge.spring.dsl;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.policy.RetryPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicyDslAndAnnotationIntegrationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                    .withUserConfiguration(PolicyConfig.class);

    @Test
    void should_apply_annotation_timeout_policy() {
        contextRunner.run(ctx -> {
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.executeResult("timeout-flow", null))
                    .expectErrorSatisfies(error -> {
                        Throwable cause = error.getCause();
                        while (cause != null && !(cause instanceof java.util.concurrent.TimeoutException)) {
                            cause = cause.getCause();
                        }
                        if (!(cause instanceof java.util.concurrent.TimeoutException)) {
                            throw new AssertionError("Expected TimeoutException in cause chain, got: " + error);
                        }
                    })
                    .verify();
        });
    }

    @Test
    void should_apply_dsl_retry_policy() {
        contextRunner.run(ctx -> {
            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);
            AtomicInteger attempts = ctx.getBean("flakyAttempts", AtomicInteger.class);
            attempts.set(0);

            StepVerifier.create(client.executeResult("retry-flow", null))
                    .expectNext("ok:7")
                    .verifyComplete();

            assertEquals(3, attempts.get());
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class PolicyConfig {

        private static final AtomicInteger FLAKY_ATTEMPTS = new AtomicInteger();

        @Bean("flakyAttempts")
        AtomicInteger flakyAttempts() {
            return FLAKY_ATTEMPTS;
        }

        @Bean
        @FlowTask(id = "slowTask", timeoutMillis = 20)
        FlowTaskHandler<Void, String> slowTask() {
            return (input, ctx) -> Mono.delay(Duration.ofMillis(100)).thenReturn("too-late");
        }

        @Bean
        @FlowTask(id = "flaky")
        FlowTaskHandler<Void, Integer> flaky() {
            return (input, ctx) -> Mono.defer(() -> {
                int attempt = FLAKY_ATTEMPTS.incrementAndGet();
                if (attempt <= 2) {
                    return Mono.error(new IllegalStateException("transient"));
                }
                return Mono.just(7);
            });
        }

        @Bean
        @FlowTask(id = "format")
        FlowTaskHandler<Integer, String> format() {
            return (input, ctx) -> Mono.just("ok:" + input);
        }

        @FlowWorkflow(id = "timeout-flow")
        @Bean
        WorkflowExecutionPlan timeoutFlow(FlowDsl dsl) {
            return dsl.flow(PolicyConfig::slowTask)
                    .build();
        }

        @FlowWorkflow(id = "retry-flow")
        @Bean
        WorkflowExecutionPlan retryFlow(FlowDsl dsl) {
            return dsl.flow(PolicyConfig::flaky)
                    .withRetry(RetryPolicy.fixed(3))
                    .then(PolicyConfig::format)
                    .build();
        }
    }
}
