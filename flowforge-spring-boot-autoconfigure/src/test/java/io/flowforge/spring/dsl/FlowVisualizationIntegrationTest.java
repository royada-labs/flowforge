package io.flowforge.spring.dsl;

import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.task.TaskDefinition;
import io.flowforge.validation.FlowValidationException;
import io.flowforge.validation.FlowValidationResult;
import io.flowforge.visualization.FlowVisualization;
import io.flowforge.visualization.FlowVisualizer;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
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

            WorkflowExecutionPlan plan = dsl.startTyped(start)
                    .then(end)
                    .build();

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

            // Invalid root: non-Void input at start should trigger MISSING_INPUT validation
            TaskDefinition<Integer, Integer> start = TaskDefinition.of("Start", Integer.class, Integer.class);

            TypedFlowBuilder<Integer> flow = dsl.startTyped(start);

            try {
                flow.build();
                fail("Should have thrown FlowValidationException");
            } catch (FlowValidationException ex) {
                WorkflowExecutionPlan plan = ex.plan().orElseThrow();
                FlowValidationResult result = ex.result();

                FlowVisualization viz = FlowVisualizer.visualize(plan, result);

                String mermaid = viz.toMermaid();
                assertTrue(mermaid.contains("class Start ff-error"), "Start node should be styled as error");
                assertTrue(mermaid.contains("classDef ff-error"), "Should include error style definition");

                String json = viz.toJson();
                assertTrue(json.contains("\"code\": \"MISSING_INPUT\""), "JSON should contain the error");
            }
        });
    }

    @Configuration
    @Import(FlowForgeAutoConfiguration.class)
    static class TestConfig {
        @Bean @FlowTask(id = "Start") TaskHandler start() { return (i, c) -> Mono.just(1); }
        @Bean @FlowTask(id = "End") TaskHandler end() { return (i, c) -> Mono.just("ok"); }

        interface TaskHandler extends io.flowforge.api.FlowTaskHandler<Object, Object> {}
    }
}
