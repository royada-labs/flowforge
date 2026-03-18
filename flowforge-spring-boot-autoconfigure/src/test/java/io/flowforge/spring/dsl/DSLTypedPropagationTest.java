package io.flowforge.spring.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.task.FlowKey;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Integration tests for the typed DSL with type propagation.
 *
 * <p>Validates that:
 * <ul>
 *   <li>TaskDefinition-based DSL produces correct workflows</li>
 *   <li>Type mismatches are caught fail-fast at definition time</li>
 *   <li>TaskDefinition.outputKey() retrieves context values end-to-end</li>
 *   <li>Typed DSL no longer relies on legacy string-based methods</li>
 * </ul>
 */
class DSLTypedPropagationTest {

    // -----------------------------------------------------------------------
    // TaskDefinition constants — the ergonomic pattern for real projects
    // -----------------------------------------------------------------------

    static TaskDefinition<Void, Integer> producer() {
        return TaskDefinition.of("Producer", Void.class, Integer.class);
    }

    static TaskDefinition<Integer, String> transformer() {
        return TaskDefinition.of("Transformer", Integer.class, String.class);
    }

    static TaskDefinition<String, String> formatter() {
        return TaskDefinition.of("Formatter", String.class, String.class);
    }

    // Intentionally incompatible with producer()'s output (Integer)
    static TaskDefinition<Boolean, String> incompatible() {
        return TaskDefinition.of("Incompatible", Boolean.class, String.class);
    }

    // -----------------------------------------------------------------------
    // Test 1: Valid typed pipeline — Producer → Transformer → Formatter
    // -----------------------------------------------------------------------

    @Test
    void typed_dsl_should_execute_valid_pipeline() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedPipelineConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure(), "Context must start");

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.<ReactiveExecutionContext>execute("typed-pipeline", null))
                            .assertNext(execCtx -> {
                                // Access via FlowKey derived from TaskDefinition
                                FlowKey<Integer> producerKey = producer().outputKey();
                                FlowKey<String> transformerKey = transformer().outputKey();
                                FlowKey<String> formatterKey = formatter().outputKey();

                                assertEquals(42, execCtx.get(producerKey).orElse(null));
                                assertEquals("val=42", execCtx.get(transformerKey).orElse(null));
                                assertEquals("[val=42]", execCtx.get(formatterKey).orElse(null));
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test 2: Type mismatch at definition time
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void typed_dsl_should_reject_incompatible_input_type_at_runtime() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedPipelineConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowDsl dsl = ctx.getBean(FlowDsl.class);

                    TypedFlowBuilder<Integer> start = dsl.startTyped(producer());

                    // Deliberately bypass generics with raw types to test runtime validation
                    TaskDefinition rawIncompatible = incompatible(); // Boolean input expected

                    assertThrows(IllegalArgumentException.class, () ->
                            ((TypedFlowBuilder) start).then(rawIncompatible)
                    );
                });
    }



    // -----------------------------------------------------------------------
    // Test 3: TaskDefinition.outputKey() → ctx.get() end-to-end
    // -----------------------------------------------------------------------

    @Test
    void output_key_should_retrieve_context_value() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, ContextIntegrationConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.<ReactiveExecutionContext>execute("context-test", null))
                            .assertNext(execCtx -> {
                                FlowKey<Integer> key = producer().outputKey();

                                Integer result = execCtx.get(key).orElse(null);
                                assertEquals(42, result);
                            })
                            .verifyComplete();
                });
    }



    // -----------------------------------------------------------------------
    // Test 5: startTyped() returns a typed builder
    // -----------------------------------------------------------------------

    @Test
    void start_typed_should_return_typed_flow_builder() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedPipelineConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowDsl dsl = ctx.getBean(FlowDsl.class);

                    TypedFlowBuilder<Integer> start = dsl.startTyped(producer());

                    assertNotNull(start, "Builder must not be null");
                });
    }


    // -----------------------------------------------------------------------
    // Configurations
    // -----------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    @Import(FlowForgeAutoConfiguration.class)
    static class ImportAutoConfig { }

    @Configuration(proxyBeanMethods = false)
    static class TypedPipelineConfig {
        @Bean ProducerTask producerTask() { return new ProducerTask(); }
        @Bean TransformerTask transformerTask() { return new TransformerTask(); }
        @Bean FormatterTask formatterTask() { return new FormatterTask(); }

        @Bean
        @FlowWorkflow(id = "typed-pipeline")
        WorkflowExecutionPlan typedPipeline(FlowDsl dsl) {
            return dsl.startTyped(producer())
                    .then(transformer())
                    .then(formatter())
                    .build();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class ContextIntegrationConfig {
        @Bean ProducerTask producerTask() { return new ProducerTask(); }

        @Bean
        @FlowWorkflow(id = "context-test")
        WorkflowExecutionPlan contextTest(FlowDsl dsl) {
            return dsl.startTyped(producer()).build();
        }

    }



    // -----------------------------------------------------------------------
    // Task implementations
    // -----------------------------------------------------------------------

    @FlowTask(id = "Producer")
    static class ProducerTask implements FlowTaskHandler<Void, Integer> {
        @Override
        public Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(42);
        }
    }

    @FlowTask(id = "Transformer")
    static class TransformerTask implements FlowTaskHandler<Integer, String> {
        @Override
        public Mono<String> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("val=" + (input == null ? 0 : input));
        }
    }

    @FlowTask(id = "Formatter")
    static class FormatterTask implements FlowTaskHandler<String, String> {
        @Override
        public Mono<String> execute(String input, ReactiveExecutionContext ctx) {
            return Mono.just("[" + (input == null ? "" : input) + "]");
        }
    }
}
