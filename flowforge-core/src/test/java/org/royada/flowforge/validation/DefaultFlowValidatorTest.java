package org.royada.flowforge.validation;

import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.plan.WorkflowPlanBuilder;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultFlowValidator} and its built-in rules.
 */
class DefaultFlowValidatorTest {

    private final DefaultFlowValidator validator = new DefaultFlowValidator();

    // -----------------------------------------------------------------------
    // Valid workflow — no errors
    // -----------------------------------------------------------------------

    @Test
    void should_pass_valid_linear_workflow() {
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A"),
                stub("C", "B")
        );

        Map<TaskId, TypeMetadata> types = Map.of(
                TaskId.of("A"), new TypeMetadata(Void.class, Integer.class),
                TaskId.of("B"), new TypeMetadata(Integer.class, String.class),
                TaskId.of("C"), new TypeMetadata(String.class, Void.class)
        );

        FlowValidationResult result = validator.validate(plan, types);

        assertTrue(result.isValid(), "Valid workflow should pass: " + result.formatted());
        assertTrue(result.errorsOnly().isEmpty());
    }

    @Test
    void should_pass_valid_workflow_without_type_metadata() {
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A")
        );

        FlowValidationResult result = validator.validate(plan);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    // -----------------------------------------------------------------------
    // TYPE_MISMATCH
    // -----------------------------------------------------------------------

    @Test
    void should_detect_type_mismatch_between_tasks() {
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A")
        );

        Map<TaskId, TypeMetadata> types = Map.of(
                TaskId.of("A"), new TypeMetadata(Void.class, Integer.class),
                TaskId.of("B"), new TypeMetadata(Boolean.class, String.class)
        );

        FlowValidationResult result = validator.validate(plan, types);

        assertFalse(result.isValid());
        assertEquals(1, result.errorsOnly().size());

        FlowValidationError error = result.errorsOnly().getFirst();
        assertEquals(FlowValidationError.TYPE_MISMATCH, error.code());
        assertEquals("B", error.taskId());
        assertTrue(error.message().contains("Boolean"), "should mention expected type");
        assertTrue(error.message().contains("Integer"), "should mention actual type");
        assertTrue(error.message().contains("A"), "should mention source task");
    }

    @Test
    void should_pass_type_check_with_assignable_types() {
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A")
        );

        Map<TaskId, TypeMetadata> types = Map.of(
                TaskId.of("A"), new TypeMetadata(Void.class, ArrayList.class),
                TaskId.of("B"), new TypeMetadata(List.class, Void.class)
        );

        FlowValidationResult result = validator.validate(plan, types);
        assertTrue(result.isValid(), "Assignable types should pass: " + result.formatted());
    }

    // -----------------------------------------------------------------------
    // MISSING_INPUT
    // -----------------------------------------------------------------------

    @Test
    void should_detect_missing_input_for_root_task() {
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A")
        );

        Map<TaskId, TypeMetadata> types = Map.of(
                TaskId.of("A"), new TypeMetadata(Integer.class, String.class),
                TaskId.of("B"), new TypeMetadata(String.class, Void.class)
        );

        FlowValidationResult result = validator.validate(plan, types);

        assertTrue(result.isValid());
        assertTrue(result.warningsOnly().stream()
                .anyMatch(e -> e.code().equals(FlowValidationError.MISSING_INPUT)
                        && e.taskId().equals("A")));
    }

    @Test
    void should_not_flag_root_with_void_input() {
        WorkflowExecutionPlan plan = buildPlan(stub("A"));

        Map<TaskId, TypeMetadata> types = Map.of(
                TaskId.of("A"), new TypeMetadata(Void.class, String.class)
        );

        FlowValidationResult result = validator.validate(plan, types);
        assertTrue(result.errorsOnly().stream()
                .noneMatch(e -> e.code().equals(FlowValidationError.MISSING_INPUT)));
    }

    // -----------------------------------------------------------------------
    // UNUSED_OUTPUT (warning)
    // -----------------------------------------------------------------------

    @Test
    void should_warn_about_unused_output_on_leaf_task() {
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A")
        );

        Map<TaskId, TypeMetadata> types = Map.of(
                TaskId.of("A"), new TypeMetadata(Void.class, Integer.class),
                TaskId.of("B"), new TypeMetadata(Integer.class, String.class)
        );

        FlowValidationResult result = validator.validate(plan, types);

        assertTrue(result.isValid());
        assertTrue(result.warningsOnly().stream()
                .anyMatch(e -> e.code().equals(FlowValidationError.UNUSED_OUTPUT)
                        && e.taskId().equals("B")));
    }

    @Test
    void should_not_warn_about_void_output_leaf() {
        WorkflowExecutionPlan plan = buildPlan(
                stub("A"),
                stub("B", "A")
        );

        Map<TaskId, TypeMetadata> types = Map.of(
                TaskId.of("A"), new TypeMetadata(Void.class, Integer.class),
                TaskId.of("B"), new TypeMetadata(Integer.class, Void.class)
        );

        FlowValidationResult result = validator.validate(plan, types);
        assertTrue(result.warningsOnly().stream()
                .noneMatch(e -> e.code().equals(FlowValidationError.UNUSED_OUTPUT)));
    }

    // -----------------------------------------------------------------------
    // Error formatting
    // -----------------------------------------------------------------------

    @Test
    void formatted_error_should_include_code_task_and_message() {
        FlowValidationError error = FlowValidationError.error(
                FlowValidationError.TYPE_MISMATCH,
                "enrichUser",
                "expected UserProfile but got Order (from 'fetchOrder')"
        );

        String formatted = error.formatted();
        assertTrue(formatted.contains("[TYPE_MISMATCH]"));
        assertTrue(formatted.contains("enrichUser"));
        assertTrue(formatted.contains("UserProfile"));
        assertTrue(formatted.contains("Order"));
    }

    @Test
    void result_formatted_should_list_all_errors() {
        FlowValidationResult result = FlowValidationResult.of(List.of(
                FlowValidationError.error("CODE_A", "taskA", "Problem A"),
                FlowValidationError.warning("CODE_B", "taskB", "Problem B")
        ));

        String formatted = result.formatted();
        assertTrue(formatted.contains("1 error(s)"));
        assertTrue(formatted.contains("1 warning(s)"));
        assertTrue(formatted.contains("[CODE_A]"));
        assertTrue(formatted.contains("[CODE_B]"));
    }

    @Test
    void exception_message_should_contain_formatted_errors() {
        FlowValidationResult result = FlowValidationResult.of(List.of(
                FlowValidationError.error(FlowValidationError.TYPE_MISMATCH, "X", "bad type")
        ));

        FlowValidationException ex = new FlowValidationException(result);
        assertTrue(ex.getMessage().contains("[TYPE_MISMATCH]"));
        assertTrue(ex.getMessage().contains("X"));
        assertSame(result, ex.result());
    }

    // -----------------------------------------------------------------------
    // Custom rule (extensibility)
    // -----------------------------------------------------------------------

    @Test
    void should_support_custom_rules() {
        FlowValidationRule noTaskCalledX = (plan, types) -> {
            List<FlowValidationError> errors = new ArrayList<>();
            for (var node : plan.nodes()) {
                if (node.id().getValue().equals("X")) {
                    errors.add(FlowValidationError.error("NO_X", "X", "Task X is forbidden"));
                }
            }
            return errors;
        };

        DefaultFlowValidator custom = new DefaultFlowValidator();
        custom.addRule(noTaskCalledX);

        WorkflowExecutionPlan plan = buildPlan(
                stub("X"),
                stub("Y", "X")
        );

        FlowValidationResult result = custom.validate(plan);
        assertTrue(result.errorsOnly().stream()
                .anyMatch(e -> e.code().equals("NO_X")));
    }

    // -----------------------------------------------------------------------
    // Helpers
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

    private record StubTask(String idValue, Set<TaskId> deps)
            implements Task<Object, Object> {

        @Override
        public TaskId id() {
            return TaskId.of(idValue);
        }

        @Override
        public Set<TaskId> dependencies() {
            return deps;
        }

        @Override
        public boolean optional() {
            return false;
        }

        @Override
        public Mono<Object> execute(Object input, ReactiveExecutionContext context) {
            return Mono.just("stub");
        }

        @Override public Class<Object> inputType() { return Object.class; }
        @Override public Class<Object> outputType() { return Object.class; }
    }

}
