package io.flowforge.visualization;

import io.flowforge.task.Task;
import io.flowforge.task.TaskId;
import io.flowforge.validation.FlowValidationError;
import io.flowforge.validation.FlowValidationResult;
import io.flowforge.validation.TypeMetadata;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FlowVisualizer} and the visualization models.
 */
class FlowVisualizerTest {

    @Test
    void should_create_visualization_from_plan_and_validation() {
        // A -> B
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A")
        );

        FlowValidationResult validation = FlowValidationResult.of(List.of(
                FlowValidationError.warning("UNUSED_OUTPUT", "B", "output not used")
        ));

        Map<String, TypeMetadata> typeInfo = Map.of(
                "A", new TypeMetadata(Void.class, Integer.class),
                "B", new TypeMetadata(Integer.class, String.class)
        );

        FlowVisualization viz = FlowVisualizer.visualize(plan, validation, typeInfo);

        // Nodes
        assertEquals(2, viz.nodes().size());
        VisualNode nodeA = viz.nodes().stream().filter(n -> n.taskId().equals("A")).findFirst().orElseThrow();
        assertEquals("Void", nodeA.inputType());
        assertEquals("Integer", nodeA.outputType());
        assertTrue(nodeA.isRoot());
        assertFalse(nodeA.isTerminal());

        VisualNode nodeB = viz.nodes().stream().filter(n -> n.taskId().equals("B")).findFirst().orElseThrow();
        assertEquals("Integer", nodeB.inputType());
        assertEquals("String", nodeB.outputType());
        assertFalse(nodeB.isRoot());
        assertTrue(nodeB.isTerminal());

        // Edges
        assertEquals(1, viz.edges().size());
        assertEquals("A", viz.edges().getFirst().from());
        assertEquals("B", viz.edges().getFirst().to());

        // Errors
        assertEquals(1, viz.errors().size());
        assertEquals("UNUSED_OUTPUT", viz.errors().getFirst().code());
        assertEquals("B", viz.errors().getFirst().taskId());
        assertEquals("WARNING", viz.errors().getFirst().severity());
    }

    @Test
    void json_export_should_produce_valid_structure() {
        VisualNode node = new VisualNode("A", "Void", "Int", true, true);
        VisualEdge edge = new VisualEdge("A", "B");
        VisualError error = new VisualError("CODE", "msg", "A", "ERROR");

        FlowVisualization viz = new FlowVisualization(List.of(node), List.of(edge), List.of(error));
        String json = viz.toJson();

        assertTrue(json.contains("\"taskId\": \"A\""));
        assertTrue(json.contains("\"from\": \"A\""));
        assertTrue(json.contains("\"to\": \"B\""));
        assertTrue(json.contains("\"severity\": \"ERROR\""));
        assertTrue(json.contains("\"root\": true"));
    }

    @Test
    void mermaid_export_should_include_styling_and_types() {
        VisualNode nodeA = new VisualNode("A", "Void", "Int", true, false);
        VisualNode nodeB = new VisualNode("B", "Int", "String", false, true);
        VisualEdge edge = new VisualEdge("A", "B");
        VisualError error = new VisualError("TYPE_MISMATCH", "oops", "B", "ERROR");

        FlowVisualization viz = new FlowVisualization(List.of(nodeA, nodeB), List.of(edge), List.of(error));
        String mermaid = viz.toMermaid();

        // Types
        assertTrue(mermaid.contains("A[A\\n(Void → Int)]"));
        assertTrue(mermaid.contains("B[B\\n(Int → String)]"));

        // Connections
        assertTrue(mermaid.contains("A --> B"));

        // Styles
        assertTrue(mermaid.contains("class B ff-error"));
        assertTrue(mermaid.contains("classDef ff-error"));
    }

    // -----------------------------------------------------------------------
    // Helpers (reused from DefaultFlowValidatorTest logic)
    // -----------------------------------------------------------------------

    private static WorkflowExecutionPlan buildPlan(Task<?, ?>... tasks) {
        return WorkflowPlanBuilder.build(Arrays.asList(tasks));
    }

    private static Task<Object, Object> stub(String id, String... deps) {
        Set<TaskId> dependencies = new LinkedHashSet<>();
        for (String dep : deps) {
            dependencies.add(TaskId.of(dep));
        }
        return new StubTask(id, dependencies);
    }

    private record StubTask(String idValue, Set<TaskId> deps) implements Task<Object, Object> {
        @Override public TaskId id() { return TaskId.of(idValue); }
        @Override public Set<TaskId> dependencies() { return deps; }
        @Override public boolean optional() { return false; }
        @Override public Mono<Object> execute(Object input, ReactiveExecutionContext context) {
            return Mono.just("stub");
        }
    }
}
