package io.flowforge.spring.dsl;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.task.TaskId;
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

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying the strictly typed DSL.
 *
 * <p>Validates that:
 * <ul>
 *   <li>Typed DSL produces the correct DAG structure</li>
 *   <li>Workflow executes correctly and results are accessible via FlowKey</li>
 * </ul>
 */
class DSLTypedIntegrationTest {

    // -----------------------------------------------------------------------
    // Task definitions
    // -----------------------------------------------------------------------
    static final TaskDefinition<Void, Integer> DEF_A = TaskDefinition.of("A", Void.class, Integer.class);
    static final TaskDefinition<Integer, Integer> DEF_B = TaskDefinition.of("B", Integer.class, Integer.class);
    static final TaskDefinition<Integer, String> DEF_C = TaskDefinition.of("C", Integer.class, String.class);

    static final FlowKey<Integer> KEY_A = DEF_A.outputKey();
    static final FlowKey<Integer> KEY_B = DEF_B.outputKey();
    static final FlowKey<String>  KEY_C = DEF_C.outputKey();

    // -----------------------------------------------------------------------
    // Test: typed DSL produces the correct DAG structure
    // -----------------------------------------------------------------------

    @Test
    void typed_dsl_should_produce_correct_dag_structure() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedDslConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure(), "Context must start successfully");

                    io.flowforge.registry.WorkflowPlanRegistry registry =
                            ctx.getBean(io.flowforge.registry.WorkflowPlanRegistry.class);

                    assertTrue(registry.contains("typed-flow"));
                    WorkflowExecutionPlan plan = registry.find("typed-flow").orElseThrow();

                    Set<String> ids = plan.nodes().stream()
                            .map(n -> n.id().getValue())
                            .collect(Collectors.toSet());
                    assertEquals(Set.of("A", "B", "C"), ids);

                    // A is root
                    Set<String> roots = plan.roots().stream()
                            .map(n -> n.id().getValue())
                            .collect(Collectors.toSet());
                    assertEquals(Set.of("A"), roots);

                    // A -> B -> C
                    assertTrue(node(plan, "B").dependencies().stream().anyMatch(d -> d.id().equals(TaskId.of("A"))));
                    assertTrue(node(plan, "C").dependencies().stream().anyMatch(d -> d.id().equals(TaskId.of("B"))));
                });
    }

    @Test
    void typed_dsl_workflow_should_execute_and_results_accessible_via_flow_key() {
        new ApplicationContextRunner()
                .withUserConfiguration(ImportAutoConfig.class, TypedDslConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());

                    FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

                    StepVerifier.create(client.<ReactiveExecutionContext>execute("typed-flow", null))
                            .assertNext(execCtx -> {
                                Integer aResult = execCtx.get(KEY_A).orElse(null);
                                Integer bResult = execCtx.get(KEY_B).orElse(null);
                                String  cResult = execCtx.get(KEY_C).orElse(null);

                                assertEquals(1, aResult, "Task A should produce 1");
                                assertEquals(10, bResult, "Task B should produce 1 + 9 = 10");
                                assertEquals("len=2", cResult, "Task C should produce 'len=2'");
                            })
                            .verifyComplete();
                });
    }

    private static io.flowforge.workflow.graph.TaskNode node(WorkflowExecutionPlan plan, String id) {
        return plan.getNode(TaskId.of(id))
                .orElseThrow(() -> new AssertionError("Missing node: " + id));
    }

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
            return dsl.startTyped(DEF_A)
                    .then(DEF_B)
                    .then(DEF_C)
                    .build();
        }
    }

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
