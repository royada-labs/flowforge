package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.annotations.FlowWorkflow;
import io.tugrandsolutions.flowforge.spring.api.FlowTaskHandler;
import io.tugrandsolutions.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.tugrandsolutions.flowforge.spring.dsl.DefaultFlowDsl;
import io.tugrandsolutions.flowforge.spring.dsl.FlowDsl;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.spring.registry.WorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowRegistrarDslIntegrationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(ImportAutoConfig.class)
                    .withUserConfiguration(DslWorkflowConfig.class);

    @Test
    void should_register_workflow_plan_from_flow_dsl_workflow_bean() {
        runner.run(ctx -> {
            assertNull(ctx.getStartupFailure(), "Context must start successfully");

            WorkflowPlanRegistry registry = ctx.getBean(WorkflowPlanRegistry.class);

            assertTrue(registry.contains("apiScore"), "WorkflowPlanRegistry must contain workflow id");
            WorkflowExecutionPlan plan = registry.find("apiScore").orElseThrow();

            // Nodes set
            Set<String> ids = plan.nodes().stream()
                    .map(n -> n.id().getValue())
                    .collect(Collectors.toSet());

            assertEquals(Set.of("A", "B", "C"), ids, "Plan must contain exactly A,B,C");

            // Roots
            Set<String> roots = plan.roots().stream()
                    .map(n -> n.id().getValue())
                    .collect(Collectors.toSet());

            assertEquals(Set.of("A"), roots, "Only A should be root");

            // Dependencies (A -> B -> C)
            assertDependsOn(plan, "B", "A");
            assertDependsOn(plan, "C", "B");

            // And no extra deps
            assertEquals(Set.of("A"), depsOf(plan, "B"));
            assertEquals(Set.of("B"), depsOf(plan, "C"));
            assertEquals(Set.of(), depsOf(plan, "A"));
        });
    }

    private static void assertDependsOn(WorkflowExecutionPlan plan, String nodeId, String dependencyId) {
        TaskNode n = node(plan, nodeId);
        boolean hasDep = n.dependencies().stream().anyMatch(d -> d.id().getValue().equals(dependencyId));
        assertTrue(hasDep, nodeId + " must depend on " + dependencyId);
    }

    private static Set<String> depsOf(WorkflowExecutionPlan plan, String nodeId) {
        return node(plan, nodeId).dependencies().stream()
                .map(d -> d.id().getValue())
                .collect(Collectors.toSet());
    }

    private static TaskNode node(WorkflowExecutionPlan plan, String id) {
        return plan.getNode(TaskId.of(id))
                .orElseThrow(() -> new AssertionError("Missing node " + id));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(FlowForgeAutoConfiguration.class)
    static class ImportAutoConfig { }

    @Configuration(proxyBeanMethods = false)
    static class DslWorkflowConfig {



        // Tasks (beans) that must be discoverable by the scanner before the workflow plan is built/registered
        @Bean TaskA taskA() { return new TaskA(); }
        @Bean TaskB taskB() { return new TaskB(); }
        @Bean TaskC taskC() { return new TaskC(); }

        @Bean
        @FlowWorkflow(id = "apiScore")
        WorkflowExecutionPlan apiScoreWorkflow(FlowDsl dsl) {
            // start/then/build nomenclature
            return dsl.start("A")
                    .then("B")
                    .then("C")
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
            return Mono.just("len=" + (input == null ? 0 : String.valueOf(input).length()));
        }
    }
}
