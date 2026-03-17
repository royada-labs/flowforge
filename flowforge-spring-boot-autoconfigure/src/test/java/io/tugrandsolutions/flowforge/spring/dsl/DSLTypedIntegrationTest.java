package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.api.FlowForgeClient;
import io.tugrandsolutions.flowforge.api.FlowTaskHandler;
import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.annotations.FlowWorkflow;
import io.tugrandsolutions.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.task.TaskRef;
import io.tugrandsolutions.flowforge.workflow.FlowKey;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying that the typed {@link TaskRef} DSL overloads
 * produce workflows identical to the string-based DSL, and that
 * {@link FlowKey} can be used to retrieve typed results from the execution context.
 */
class DSLTypedIntegrationTest {

    // -----------------------------------------------------------------------
    // Shared typed refs — defined as constants (how users would use them)
    // -----------------------------------------------------------------------
    static final TaskRef<Integer> REF_A = TaskRef.of("A", Integer.class);
    static final TaskRef<Integer> REF_B = TaskRef.of("B", Integer.class);
    static final TaskRef<String>  REF_C = TaskRef.of("C", String.class);

    static final FlowKey<Integer> KEY_A = FlowKey.of("A", Integer.class);
    static final FlowKey<Integer> KEY_B = FlowKey.of("B", Integer.class);
    static final FlowKey<String>  KEY_C = FlowKey.of("C", String.class);

    // -----------------------------------------------------------------------
    // Test: typed DSL produces the correct DAG structure
    // -----------------------------------------------------------------------

    @Test
    void typed_dsl_should_produce_correct_dag_structure() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedDslConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure(), "Context must start successfully");

                    io.tugrandsolutions.flowforge.registry.WorkflowPlanRegistry registry =
                            ctx.getBean(io.tugrandsolutions.flowforge.registry.WorkflowPlanRegistry.class);

                    assertTrue(registry.contains("typed-flow"));
                    WorkflowExecutionPlan plan = registry.find("typed-flow").orElseThrow();

                    Set<String> ids = plan.nodes().stream()
                            .map(n -> n.id().getValue())
                            .collect(Collectors.toSet());
                    assertEquals(Set.of("A", "B", "C"), ids);

                    // A is root (no deps)
                    Set<String> roots = plan.roots().stream()
                            .map(n -> n.id().getValue())
                            .collect(Collectors.toSet());
                    assertEquals(Set.of("A"), roots);

                    // A -> B -> C
                    WorkflowExecutionPlan fp = plan;
                    assertTrue(node(fp, "B").dependencies().stream().anyMatch(d -> d.id().equals(TaskId.of("A"))));
                    assertTrue(node(fp, "C").dependencies().stream().anyMatch(d -> d.id().equals(TaskId.of("B"))));
                });
    }

    // -----------------------------------------------------------------------
    // Test: typed DSL workflow executes correctly and results are accessible
    // -----------------------------------------------------------------------

    @Test
    void typed_dsl_workflow_should_execute_and_results_accessible_via_flow_key() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedDslConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.execute("typed-flow", null))
                            .assertNext(execCtx -> {
                                // Typed retrieval via FlowKey
                                Integer aResult = execCtx.get(KEY_A).orElse(null);
                                Integer bResult = execCtx.get(KEY_B).orElse(null);
                                String  cResult = execCtx.get(KEY_C).orElse(null);

                                assertEquals(1, aResult, "Task A should produce 1");
                                assertEquals(10, bResult, "Task B should produce 1 + 9 = 10");
                                assertEquals("len=2", cResult, "Task C should produce 'len=2' (length of '10')");
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test: typed DSL and string DSL produce equivalent workflows
    // -----------------------------------------------------------------------

    @Test
    void typed_dsl_and_string_dsl_should_coexist_and_produce_equivalent_results() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, MixedDslConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    // Execute typed-only workflow
                    StepVerifier.create(client.execute("typed-only", null))
                            .assertNext(execCtx ->
                                    assertEquals(1, execCtx.get(KEY_A).orElse(null)))
                            .verifyComplete();

                    // Execute string-only workflow
                    StepVerifier.create(client.execute("string-only", null))
                            .assertNext(execCtx ->
                                    assertEquals(1, execCtx.get(KEY_A).orElse(null)))
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Test: mixed typed/string DSL in the same workflow definition
    // -----------------------------------------------------------------------

    @Test
    void mixed_typed_and_string_dsl_should_build_valid_workflow() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, MixedInlineConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.execute("mixed-flow", null))
                            .assertNext(execCtx -> {
                                // A and B both present means the mixed flow ran
                                assertTrue(execCtx.get(KEY_A).isPresent());
                                assertTrue(execCtx.get(KEY_B).isPresent());
                            })
                            .verifyComplete();
                });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static io.tugrandsolutions.flowforge.workflow.graph.TaskNode node(WorkflowExecutionPlan plan, String id) {
        return plan.getNode(TaskId.of(id))
                .orElseThrow(() -> new AssertionError("Missing node: " + id));
    }

    // -----------------------------------------------------------------------
    // Configurations
    // -----------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    @Import(FlowForgeAutoConfiguration.class)
    static class ImportAutoConfig { }

    @Configuration(proxyBeanMethods = false)
    static class TypedDslConfig {
        @Bean TaskA taskA() { return new TaskA(); }
        @Bean TaskB taskB() { return new TaskB(); }
        @Bean TaskC taskC() { return new TaskC(); }

        @Bean
        @FlowWorkflow(id = "typed-flow")
        WorkflowExecutionPlan typedFlow(FlowDsl dsl) {
            // Full typed DSL — using TaskRef overloads
            return dsl.start(REF_A)
                    .then(REF_B)
                    .then(REF_C)
                    .build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MixedDslConfig {
        @Bean TaskA taskA() { return new TaskA(); }
        @Bean TaskB taskB() { return new TaskB(); }

        @Bean
        @FlowWorkflow(id = "typed-only")
        WorkflowExecutionPlan typedOnly(FlowDsl dsl) {
            return dsl.start(REF_A).then(REF_B).build();
        }

        @Bean
        @FlowWorkflow(id = "string-only")
        WorkflowExecutionPlan stringOnly(FlowDsl dsl) {
            return dsl.start("A").then("B").build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MixedInlineConfig {
        @Bean TaskA taskA() { return new TaskA(); }
        @Bean TaskB taskB() { return new TaskB(); }

        @Bean
        @FlowWorkflow(id = "mixed-flow")
        WorkflowExecutionPlan mixedFlow(FlowDsl dsl) {
            // Mix: start with TaskRef, then with string
            return dsl.start(REF_A).then("B").build();
        }
    }

    // -----------------------------------------------------------------------
    // Task implementations (same as in existing integration tests)
    // -----------------------------------------------------------------------

    @FlowTask(id = "A")
    static class TaskA implements FlowTaskHandler<Void, Integer> {
        @Override public Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(1);
        }
    }

    @FlowTask(id = "B")
    static class TaskB implements FlowTaskHandler<Integer, Integer> {
        @Override public Mono<Integer> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just((input == null ? 0 : input) + 9);
        }
    }

    @FlowTask(id = "C")
    static class TaskC implements FlowTaskHandler<Integer, String> {
        @Override public Mono<String> execute(Integer input, ReactiveExecutionContext ctx) {
            return Mono.just("len=" + (input == null ? "0" : String.valueOf(input)).length());
        }
    }
}
