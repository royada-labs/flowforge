package io.flowforge.spring.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Integration tests for the DAG validation system integrated with the typed DSL.
 *
 * <p>Validates that:
 * <ul>
 *   <li>Valid typed workflows pass validation and execute correctly</li>
 *   <li>Type mismatches are caught fail-fast at DSL definition time</li>
 *   <li>Root tasks requiring input are validated at execution time</li>
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

    // Root that requires input.
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

                    StepVerifier.create(client.<ReactiveExecutionContext>execute("valid-pipeline", null))
                            .assertNext(execCtx -> {
                                assertEquals(42, execCtx.get(producer().outputKey()).orElse(null));
                                assertEquals("val=42", execCtx.get(transformer().outputKey()).orElse(null));
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test 2: Type mismatch detected immediately during .then(...)
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void type_mismatch_should_throw_illegal_argument_exception() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, ValidPipelineConfig.class, BadTransformerTaskConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowDsl dsl = ctx.getBean(FlowDsl.class);

                    TypedFlowBuilder start = dsl.startTyped(producer());
                    TaskDefinition rawBad = badTransformer();

                    IllegalArgumentException ex = assertThrows(
                            IllegalArgumentException.class,
                            () -> start.then(rawBad)
                    );

                    assertTrue(ex.getMessage().contains("BadTransformer"),
                            "Should mention the problematic task: " + ex.getMessage());
                });
    }

    // -----------------------------------------------------------------------
    // Test 3: Root task requiring input must receive initial input at execution time
    // -----------------------------------------------------------------------

    @Test
    void missing_input_should_fail_at_execution_with_clear_error() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, MissingInputConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure(), "Context should start");

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    IllegalArgumentException ex = assertThrows(
                            IllegalArgumentException.class,
                            () -> client.executeResult("missing-input", null)
                    );
                    assertNotNull(ex.getMessage());
                    assertTrue(ex.getMessage().contains("requires initial input"));

                    StepVerifier.create(client.executeResult("missing-input", 99))
                            .expectNext("got=99")
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test 5: Root-input validation warning does not block build
    // -----------------------------------------------------------------------

    @Test
    void root_input_validation_warning_should_not_block_build() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, ValidPipelineConfig.class, BadTransformerTaskConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    FlowDsl dsl = ctx.getBean(FlowDsl.class);
                    // should not throw after MissingInput is downgraded to warning
                    dsl.startTyped(needsInput()).build();
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
        @Bean NeedsInputTask needsInputTask() { return new NeedsInputTask(); }

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
