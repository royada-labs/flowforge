package io.flowforge.spring.dsl;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.dsl.TypedTaskNode;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.task.TaskDefinition;
import io.flowforge.task.FlowKey;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the typed DSL with type propagation.
 *
 * <p>Validates that:
 * <ul>
 *   <li>TaskDefinition-based DSL produces correct workflows</li>
 *   <li>Type mismatches are caught fail-fast at definition time</li>
 *   <li>TypedTaskNode → FlowKey → context retrieval works end-to-end</li>
 *   <li>Typed DSL coexists with string-based DSL</li>
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

                    StepVerifier.create(client.execute("typed-pipeline", null))
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
        // NOTE: The primary type-safety mechanism is compile-time generics.
        // The compiler ALREADY prevents calling then(incompatible(), start.node())
        // because TypedTaskNode<Integer> cannot match TypedTaskNode<Boolean>.
        //
        // This test validates the RUNTIME fail-fast path that catches mismatches
        // when the API is used via raw types, reflection, or dynamically constructed
        // definitions.
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedPipelineConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowDsl dsl = ctx.getBean(FlowDsl.class);

                    TypedFlowBuilder<Integer> start = dsl.startTyped(producer());

                    // Deliberately bypass generics with raw types to test runtime validation
                    TaskDefinition rawIncompatible = incompatible(); // Boolean input expected
                    TypedTaskNode rawNode = start.node();            // Integer output

                    assertThrows(IllegalArgumentException.class, () ->
                            start.untyped().then(rawIncompatible, rawNode)
                    );
                });
    }


    // -----------------------------------------------------------------------
    // Test 3: TypedTaskNode → FlowKey → ctx.get() end-to-end
    // -----------------------------------------------------------------------

    @Test
    void typed_task_node_toKey_should_retrieve_context_value() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, ContextIntegrationConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.execute("context-test", null))
                            .assertNext(execCtx -> {
                                // Simulate what a user would do: use the node reference they got from DSL
                                TypedTaskNode<Integer> producerNode = new TypedTaskNode<>(producer().toRef());
                                FlowKey<Integer> key = producerNode.outputKey();

                                Integer result = execCtx.get(key).orElse(null);
                                assertEquals(42, result);
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test 4: Mixed typed and string DSL in the same workflow
    // -----------------------------------------------------------------------

    @Test
    void mixed_typed_and_string_dsl_should_coexist() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, MixedDslConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.execute("mixed-pipeline", null))
                            .assertNext(execCtx -> {
                                assertEquals(42, execCtx.get(producer().outputKey()).orElse(null));
                                assertEquals("val=42", execCtx.get(transformer().outputKey()).orElse(null));
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test 5: startTyped returns both builder and typed node
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    void startTyped_should_return_typed_flow_builder() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedPipelineConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowDsl dsl = ctx.getBean(FlowDsl.class);

                    TypedFlowBuilder<Integer> start = dsl.startTyped(producer());

                    assertNotNull(start.untyped(), "Builder must not be null");
                    assertNotNull(start.node(), "Node must not be null");
                    assertEquals("Producer", start.node().ref().idValue());
                    assertEquals(Integer.class, start.node().ref().outputType());
                    
                    // Verify legacy still works for now
                    FlowDsl.TypedFlowStart<Integer> legacy = new FlowDsl.TypedFlowStart<>(start.untyped(), start.node());
                    assertNotNull(legacy);
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
            return dsl.start("Producer").build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MixedDslConfig {
        @Bean ProducerTask producerTask() { return new ProducerTask(); }
        @Bean TransformerTask transformerTask() { return new TransformerTask(); }

        @Bean
        @FlowWorkflow(id = "mixed-pipeline")
        WorkflowExecutionPlan mixedPipeline(FlowDsl dsl) {
            // Start typed, then chain with string
            return dsl.startTyped(producer())
                    .untyped()
                    .then("Transformer")
                    .build();
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
