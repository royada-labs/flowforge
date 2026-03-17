package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.validation.FlowValidationException;
import io.tugrandsolutions.flowforge.validation.FlowValidationResult;
import io.tugrandsolutions.flowforge.visualization.FlowVisualization;
import io.tugrandsolutions.flowforge.visualization.FlowVisualizer;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for FlowVisualizer working with the FlowForge DSL.
 */
class FlowVisualizationIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void should_visualize_valid_workflow_with_types() {
        contextRunner.run(ctx -> {
            FlowDsl dsl = ctx.getBean(FlowDsl.class);

            TaskDefinition<Void, Integer> start = TaskDefinition.of("Start", Void.class, Integer.class);
            TaskDefinition<Integer, String> end = TaskDefinition.of("End", Integer.class, String.class);

            var flow = dsl.startTyped(start);
            flow.builder().then(end, flow.node());
            WorkflowExecutionPlan plan = flow.builder().build();

            FlowVisualization viz = FlowVisualizer.visualize(plan, FlowValidationResult.of(java.util.List.of()));

            assertNotNull(viz.toJson());
            assertTrue(viz.toMermaid().contains("Start"));
            assertTrue(viz.toMermaid().contains("End"));
        });
    }

    @Test
    void should_visualize_broken_workflow_from_exception() {
        contextRunner.run(ctx -> {
            FlowDsl dsl = ctx.getBean(FlowDsl.class);

            TaskDefinition<Void, Integer> start = TaskDefinition.of("Start", Void.class, Integer.class);
            // Incompatible type: expects Boolean
            TaskDefinition<Boolean, String> end = TaskDefinition.of("End", Boolean.class, String.class);

            var flow = dsl.startTyped(start);
            // Use single-param then() to bypass local fail-fast and let global validator catch it
            flow.builder().then(end);

            try {
                flow.builder().build();
                fail("Should have thrown FlowValidationException");
            } catch (FlowValidationException ex) {
                WorkflowExecutionPlan plan = ex.plan().orElseThrow();
                FlowValidationResult result = ex.result();

                FlowVisualization viz = FlowVisualizer.visualize(plan, result);

                String mermaid = viz.toMermaid();
                assertTrue(mermaid.contains("class End ff-error"), "End node should be styled as error");
                assertTrue(mermaid.contains("classDef ff-error"), "Should include error style definition");

                String json = viz.toJson();
                assertTrue(json.contains("\"code\": \"TYPE_MISMATCH\""), "JSON should contain the error");
            }
        });
    }

    @Configuration
    @Import(FlowForgeAutoConfiguration.class)
    static class TestConfig {
        @Bean @FlowTask(id = "Start") TaskHandler start() { return (i, c) -> Mono.just(1); }
        @Bean @FlowTask(id = "End") TaskHandler end() { return (i, c) -> Mono.just("ok"); }

        interface TaskHandler extends io.tugrandsolutions.flowforge.api.FlowTaskHandler<Object, Object> {}
    }
}
