package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.api.FlowForgeClient;
import io.tugrandsolutions.flowforge.api.FlowTaskHandler;
import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.annotations.FlowWorkflow;
import io.tugrandsolutions.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.validation.FlowValidationException;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the DAG validation system integrated with the typed DSL.
 *
 * <p>Validates that:
 * <ul>
 *   <li>Valid typed workflows pass validation and execute correctly</li>
 *   <li>Type mismatches are caught as FlowValidationException at build() time</li>
 *   <li>Missing input is detected for root tasks</li>
 *   <li>Legacy string-based workflows still work (backward compatibility)</li>
 * </ul>
 */
class DSLValidationIntegrationTest {

    // -----------------------------------------------------------------------
    // Task definitions
    // -----------------------------------------------------------------------

    static TaskDefinition<Void, Integer> producer() {
        return TaskDefinition.of("Producer", Void.class, Integer.class);
    }

    static TaskDefinition<Integer, String> transformer() {
        return TaskDefinition.of("Transformer", Integer.class, String.class);
    }

    static TaskDefinition<String, Void> sink() {
        return TaskDefinition.of("Sink", String.class, Void.class);
    }

    // Intentionally incompatible: expects Boolean but upstream produces Integer
    static TaskDefinition<Boolean, String> badTransformer() {
        return TaskDefinition.of("BadTransformer", Boolean.class, String.class);
    }

    // Root that requires input — should trigger MISSING_INPUT
    static TaskDefinition<Integer, String> needsInput() {
        return TaskDefinition.of("NeedsInput", Integer.class, String.class);
    }

    // -----------------------------------------------------------------------
    // Test 1: Valid pipeline passes validation and executes
    // -----------------------------------------------------------------------

    @Test
    void valid_typed_pipeline_should_pass_validation_and_execute() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, ValidPipelineConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure(), "Context must start");

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.execute("valid-pipeline", null))
                            .assertNext(execCtx -> {
                                assertEquals(42, execCtx.get(producer().outputKey()).orElse(null));
                                assertEquals("val=42", execCtx.get(transformer().outputKey()).orElse(null));
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test 2: Type mismatch detected at build() → FlowValidationException
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void type_mismatch_should_throw_validation_exception_at_build() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, ValidPipelineConfig.class, BadTransformerTaskConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowDsl dsl = ctx.getBean(FlowDsl.class);

                    // Use single-param then(TaskDefinition) to bypass local check
                    // and let the global validator find the edge mismatch.
                    TypedFlowBuilder start = dsl.startTyped(producer());
                    TaskDefinition rawBad = badTransformer();
                    start.untyped().then(rawBad);

                    // build() should throw FlowValidationException due to TYPE_MISMATCH
                    FlowValidationException ex = assertThrows(
                            FlowValidationException.class,
                            () -> start.build()
                    );

                    assertTrue(ex.getMessage().contains("TYPE_MISMATCH"),
                            "Should contain TYPE_MISMATCH: " + ex.getMessage());
                    assertTrue(ex.getMessage().contains("BadTransformer"),
                            "Should mention the problematic task: " + ex.getMessage());
                });
    }

    // -----------------------------------------------------------------------
    // Test 3: Missing input detected for root task
    // -----------------------------------------------------------------------

    @Test
    void missing_input_should_throw_validation_exception() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, MissingInputConfig.class)
                .run(ctx -> {
                    // The workflow definition happens during bean creation,
                    // so the context startup itself should fail
                    assertNotNull(ctx.getStartupFailure(),
                            "Context should fail due to MISSING_INPUT validation");
                    assertTrue(ctx.getStartupFailure().getCause() instanceof FlowValidationException
                                    || ctx.getStartupFailure().getMessage().contains("MISSING_INPUT"),
                            "Should be caused by FlowValidationException");
                });
    }

    // -----------------------------------------------------------------------
    // Test 4: Legacy string-based workflow passes (backward compat)
    // -----------------------------------------------------------------------

    @Test
    void legacy_string_workflow_should_pass_validation() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, LegacyConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure(), "Legacy workflow should pass validation");

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.execute("legacy-pipeline", null))
                            .assertNext(execCtx -> {
                                assertEquals(42,
                                        execCtx.get(producer().outputKey()).orElse(null));
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test 5: Validation exception has well-formatted message
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void validation_exception_should_have_formatted_message() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, ValidPipelineConfig.class, BadTransformerTaskConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowDsl dsl = ctx.getBean(FlowDsl.class);

                    TypedFlowBuilder start = dsl.startTyped(producer());
                    TaskDefinition rawBad = badTransformer();
                    start.untyped().then(rawBad);

                    try {
                        start.build();
                        fail("Should throw FlowValidationException");
                    } catch (FlowValidationException ex) {
                        String msg = ex.getMessage();
                        // Verify compiler-style formatting
                        assertTrue(msg.contains("[TYPE_MISMATCH]"), "Format: " + msg);
                        assertTrue(msg.contains("error(s)"), "Format: " + msg);
                        assertNotNull(ex.result());
                        assertFalse(ex.result().isValid());
                    }
                });
    }

    // -----------------------------------------------------------------------
    // Configurations
    // -----------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    @Import(FlowForgeAutoConfiguration.class)
    static class ImportAutoConfig { }

    @Configuration(proxyBeanMethods = false)
    static class ValidPipelineConfig {
        @Bean ProducerTask producerTask() { return new ProducerTask(); }
        @Bean TransformerTask transformerTask() { return new TransformerTask(); }

        @Bean
        @FlowWorkflow(id = "valid-pipeline")
        WorkflowExecutionPlan validPipeline(FlowDsl dsl) {
            return dsl.startTyped(producer())
                    .then(transformer())
                    .build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BadTransformerTaskConfig {
        @Bean BadTransformerTaskImpl badTransformerTask() { return new BadTransformerTaskImpl(); }
    }

    @Configuration(proxyBeanMethods = false)
    static class MissingInputConfig {
        @Bean NeedsInputTask needsInputTask() { return new NeedsInputTask(); }

        @Bean
        @FlowWorkflow(id = "missing-input")
        WorkflowExecutionPlan missingInputPipeline(FlowDsl dsl) {
            return dsl.startTyped(needsInput()).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LegacyConfig {
        @Bean ProducerTask legacyProducer() { return new ProducerTask(); }
        @Bean TransformerTask legacyTransformer() { return new TransformerTask(); }

        @Bean
        @FlowWorkflow(id = "legacy-pipeline")
        WorkflowExecutionPlan legacyPipeline(FlowDsl dsl) {
            return dsl.start("Producer").then("Transformer").build();
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

    @FlowTask(id = "BadTransformer")
    static class BadTransformerTaskImpl implements FlowTaskHandler<Boolean, String> {
        @Override
        public Mono<String> execute(Boolean input, ReactiveExecutionContext ctx) {
            return Mono.just("bad");
        }
    }

    @FlowTask(id = "NeedsInput")
    static class NeedsInputTask implements FlowTaskHandler<Integer, String> {
        @Override
        public Mono<String> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("got=" + input);
        }
    }
}
