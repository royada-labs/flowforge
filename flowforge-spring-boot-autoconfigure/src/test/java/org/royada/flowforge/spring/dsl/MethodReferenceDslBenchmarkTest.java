package org.royada.flowforge.spring.dsl;

import org.royada.flowforge.api.FlowForgeClient;
import org.royada.flowforge.api.FlowTaskHandler;
import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.annotations.TaskHandler;
import org.royada.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Manual benchmark to compare runtime execution cost between:
 * - Typed TaskDefinition DSL
 * - Method reference DSL
 *
 * Run manually:
 * ./gradlew :flowforge-spring-boot-autoconfigure:test --tests "org.royada.flowforge.spring.dsl.MethodReferenceDslBenchmarkTest"
 */
@Disabled("Manual benchmark - not for CI")
class MethodReferenceDslBenchmarkTest {

    @Test
    void compare_runtime_overhead() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                .withUserConfiguration(BenchmarkConfig.class)
                .run(ctx -> {
                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    int warmup = 200;
                    int iterations = 2000;

                    for (int i = 0; i < warmup; i++) {
                        client.executeResult("bench-manual", null).block(Duration.ofSeconds(5));
                        client.executeResult("bench-method-ref", null).block(Duration.ofSeconds(5));
                    }

                    long tManual = measure(client, "bench-manual", iterations);
                    long tMethodRef = measure(client, "bench-method-ref", iterations);
                    long tAnnotatedRef = measure(client, "bench-annotated-ref", iterations);

                    double avgManualUs = (tManual / 1000.0) / iterations;
                    double avgMethodRefUs = (tMethodRef / 1000.0) / iterations;
                    double avgAnnotatedRefUs = (tAnnotatedRef / 1000.0) / iterations;

                    System.out.printf("bench-manual avg_us=%.2f%n", avgManualUs);
                    System.out.printf("bench-method-ref avg_us=%.2f%n", avgMethodRefUs);
                    System.out.printf("bench-annotated-ref avg_us=%.2f%n", avgAnnotatedRefUs);
                    System.out.printf("ratio method_ref/manual=%.4f%n", avgMethodRefUs / avgManualUs);
                    System.out.printf("ratio annotated_ref/manual=%.4f%n", avgAnnotatedRefUs / avgManualUs);
                });
    }

    private static long measure(FlowForgeClient client, String workflowId, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            client.executeResult(workflowId, null).block(Duration.ofSeconds(5));
        }
        return System.nanoTime() - start;
    }

    @Configuration(proxyBeanMethods = false)
    static class BenchmarkConfig {
        static final TaskDefinition<Void, Integer> A = TaskDefinition.of("benchA", Void.class, Integer.class);
        static final TaskDefinition<Integer, String> B = TaskDefinition.of("benchB", Integer.class, String.class);
        static final TaskDefinition<String, Integer> C = TaskDefinition.of("benchC", String.class, Integer.class);

        @Bean
        @FlowTask(id = "benchA")
        FlowTaskHandler<Void, Integer> benchA() {
            return (input, ctx) -> Mono.just(42);
        }

        @Bean
        @FlowTask(id = "benchB")
        FlowTaskHandler<Integer, String> benchB() {
            return (input, ctx) -> Mono.just("v=" + input);
        }

        @Bean
        @FlowTask(id = "benchC")
        FlowTaskHandler<String, Integer> benchC() {
            return (input, ctx) -> Mono.just(input.length());
        }

        @Bean
        @FlowWorkflow(id = "bench-manual")
        WorkflowExecutionPlan manual(FlowDsl dsl) {
            return dsl.start(A).then(B).then(C).build();
        }

        @Bean
        @FlowWorkflow(id = "bench-method-ref")
        WorkflowExecutionPlan methodRef(FlowDsl dsl) {
            return dsl.flow(BenchmarkConfig::benchA)
                    .then(BenchmarkConfig::benchB)
                    .then(BenchmarkConfig::benchC)
                    .build();
        }

        @Bean
        Ops ops() {
            return new Ops();
        }

        @Bean
        @FlowWorkflow(id = "bench-annotated-ref")
        WorkflowExecutionPlan annotatedRef(FlowDsl dsl) {
            return dsl.flow(Ops::a)
                    .then(Ops::b)
                    .then(Ops::c)
                    .build();
        }
    }

    @TaskHandler("bench")
    static class Ops {
        @FlowTask(id = "benchOpsA")
        public Mono<Integer> a(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(42);
        }

        @FlowTask(id = "benchOpsB")
        public Mono<String> b(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("v=" + input);
        }

        @FlowTask(id = "benchOpsC")
        public Mono<Integer> c(String input, ReactiveExecutionContext ctx) {
            return Mono.just(input.length());
        }
    }
}
