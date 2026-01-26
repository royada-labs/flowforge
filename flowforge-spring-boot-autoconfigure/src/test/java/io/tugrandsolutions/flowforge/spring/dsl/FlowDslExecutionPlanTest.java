package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.spring.registry.TaskProvider;
import io.tugrandsolutions.flowforge.task.Task;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import io.tugrandsolutions.flowforge.workflow.graph.TaskNode;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FlowDslExecutionPlanTest {

    @Test
    void linear_A_then_B_then_C() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(providerNoop("A"));
        reg.register(providerNoop("B"));
        reg.register(providerNoop("C"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        WorkflowExecutionPlan plan = dsl
                .start("A")
                .then("B")
                .then("C")
                .build();

        assertRootIds(plan, Set.of("A"));
        assertDependsOn(plan, "B", Set.of("A"));
        assertDependsOn(plan, "C", Set.of("B"));
        assertDependents(plan, "A", Set.of("B"));
        assertDependents(plan, "B", Set.of("C"));
        assertDependents(plan, "C", Set.of());
    }

    @Test
    void fork_A_to_B_and_C() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(providerNoop("A"));
        reg.register(providerNoop("B"));
        reg.register(providerNoop("C"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        WorkflowExecutionPlan plan = dsl
                .start("A")
                .fork(
                        b -> b.then("B"),
                        b -> b.then("C")
                )
                .build();

        assertRootIds(plan, Set.of("A"));
        assertDependsOn(plan, "B", Set.of("A"));
        assertDependsOn(plan, "C", Set.of("A"));
        assertDependents(plan, "A", Set.of("B", "C"));
    }

    @Test
    void fork_join_A_to_BC_then_D() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(providerNoop("A"));
        reg.register(providerNoop("B"));
        reg.register(providerNoop("C"));
        reg.register(providerNoop("D"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        WorkflowExecutionPlan plan = dsl
                .start("A")
                .fork(
                        b -> b.then("B"),
                        b -> b.then("C")
                )
                .join("D")
                .build();

        assertRootIds(plan, Set.of("A"));
        assertDependsOn(plan, "B", Set.of("A"));
        assertDependsOn(plan, "C", Set.of("A"));
        assertDependsOn(plan, "D", Set.of("B", "C"));

        assertDependents(plan, "A", Set.of("B", "C"));
        assertDependents(plan, "B", Set.of("D"));
        assertDependents(plan, "C", Set.of("D"));
    }

    @Test
    void fork_with_sequences_A_to_B_to_C_and_D_then_E() {
        TaskHandlerRegistry reg = new TaskHandlerRegistry();
        reg.register(providerNoop("A"));
        reg.register(providerNoop("B"));
        reg.register(providerNoop("C"));
        reg.register(providerNoop("D"));
        reg.register(providerNoop("E"));

        FlowDsl dsl = new DefaultFlowDsl(reg);

        WorkflowExecutionPlan plan = dsl
                .start("A")
                .fork(
                        br -> br.then("B").then("C"),
                        br -> br.then("D")
                )
                .join("E")
                .build();

        assertRootIds(plan, Set.of("A"));

        assertDependsOn(plan, "B", Set.of("A"));
        assertDependsOn(plan, "C", Set.of("B"));

        assertDependsOn(plan, "D", Set.of("A"));

        assertDependsOn(plan, "E", Set.of("C", "D"));
        assertDependents(plan, "C", Set.of("E"));
        assertDependents(plan, "D", Set.of("E"));
    }

    /* ========================= */
    /* ======== helpers ======== */
    /* ========================= */

    private static TaskNode node(WorkflowExecutionPlan plan, String id) {
        return plan.getNode(new TaskId(id))
                .orElseThrow(() -> new AssertionError("Missing node: " + id));
    }

    private static void assertRootIds(WorkflowExecutionPlan plan, Set<String> expectedRootIds) {
        Set<String> roots = plan.roots().stream()
                .map(n -> n.id().getValue())
                .collect(Collectors.toSet());
        assertEquals(expectedRootIds, roots, "roots mismatch");
    }

    private static void assertDependsOn(WorkflowExecutionPlan plan, String nodeId, Set<String> expectedDependencies) {
        Set<String> deps = node(plan, nodeId).dependencies().stream()
                .map(n -> n.id().getValue())
                .collect(Collectors.toSet());
        assertEquals(expectedDependencies, deps, "dependencies mismatch for " + nodeId);
    }

    private static void assertDependents(WorkflowExecutionPlan plan, String nodeId, Set<String> expectedDependents) {
        Set<String> deps = node(plan, nodeId).dependents().stream()
                .map(n -> n.id().getValue())
                .collect(Collectors.toSet());
        assertEquals(expectedDependents, deps, "dependents mismatch for " + nodeId);
    }

    private static TaskProvider<Object, Object> providerNoop(String id) {
        TaskId taskId = new TaskId(id);
        return new TaskProvider<>() {
            @Override public TaskId id() { return taskId; }

            @Override
            public Task<Object, Object> get() {
                return noop(id);
            }
        };
    }

    private static Task<Object, Object> noop(String id) {
        return new Task<>() {
            @Override public TaskId id() { return new TaskId(id); }
            @Override public Set<TaskId> dependencies() { return Set.of(); }
            @Override public boolean optional() { return false; }
            @Override public Mono<Object> execute(Object input, ReactiveExecutionContext ctx) {
                return Mono.just("ok");
            }
        };
    }
}
