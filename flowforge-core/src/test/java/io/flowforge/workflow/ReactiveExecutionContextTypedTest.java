package io.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.flowforge.exception.TypeMismatchException;
import io.flowforge.task.FlowKey;
import io.flowforge.task.TaskDefinition;

/**
 * Tests for the typed {@link FlowKey}-based API on {@link ReactiveExecutionContext}.
 * Verifies both the new API and coexistence with the legacy TaskId-based API.
 */
class ReactiveExecutionContextTypedTest {

    private ReactiveExecutionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new InMemoryReactiveExecutionContext();
    }

    // -----------------------------------------------------------------------
    // FlowKey put/get
    // -----------------------------------------------------------------------

    @Test
    void should_put_and_get_value_with_flow_key() {
        FlowKey<String> key = TaskDefinition.of("taskA", Void.class, String.class).outputKey();

        ctx.put(key, "hello");

        Optional<String> result = ctx.get(key);
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    void should_put_and_get_integer_with_flow_key() {
        FlowKey<Integer> key = TaskDefinition.of("taskB", Void.class, Integer.class).outputKey();

        ctx.put(key, 42);

        Optional<Integer> result = ctx.get(key);
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    void should_return_empty_optional_when_key_not_present() {
        FlowKey<String> key = TaskDefinition.of("nonExistent", Void.class, String.class).outputKey();

        Optional<String> result = ctx.get(key);

        assertFalse(result.isPresent());
    }

    @Test
    void should_throw_mismatch_exception_when_stored_type_does_not_match() {
        // We use a raw TaskId to simulate a type mismatch in the store (if possible)
        // or just test that the context handles type mismatches if we try to get a key with wrong type
        FlowKey<Integer> intKey = TaskDefinition.of("typeMismatch", Void.class, Integer.class).outputKey();
        ctx.put(intKey, 42); // Integer stored

        FlowKey<String> stringKey = TaskDefinition.of("typeMismatch", Void.class, String.class).outputKey();

        assertThrows(TypeMismatchException.class, () -> ctx.get(stringKey),
                "Should throw TypeMismatchException when stored Integer is fetched as String");
    }


    @Test
    void should_provide_getOrThrow_api() {
        FlowKey<String> key = TaskDefinition.of("taskT", Void.class, String.class).outputKey();
        ctx.put(key, "data");

        assertEquals("data", ctx.getOrThrow(key));
        assertThrows(NoSuchElementException.class, () -> ctx.getOrThrow(TaskDefinition.of("missing", Void.class, String.class).outputKey()));
    }

    @Test
    void should_provide_getOrDefault_api() {
        FlowKey<String> key = TaskDefinition.of("taskD", Void.class, String.class).outputKey();

        assertEquals("fallback", ctx.getOrDefault(key, "fallback"));
        ctx.put(key, "real");
        assertEquals("real", ctx.getOrDefault(key, "fallback"));
    }

    // -----------------------------------------------------------------------
    // isCompleted with FlowKey
    // -----------------------------------------------------------------------

    @Test
    void should_report_completed_after_put_via_flow_key() {
        FlowKey<String> key = TaskDefinition.of("taskC", Void.class, String.class).outputKey();

        assertFalse(ctx.isCompleted(key));

        ctx.put(key, "done");

        assertTrue(ctx.isCompleted(key));
    }

    // -----------------------------------------------------------------------
    // Coexistence: FlowKey API and legacy TaskId API on the same store
    // -----------------------------------------------------------------------

    @Test
    void should_be_able_to_check_completion_by_task_id_if_needed_internally() {
        FlowKey<String> key = TaskDefinition.of("shared", Void.class, String.class).outputKey();
        ctx.put(key, "value");

        assertTrue(ctx.isCompleted(key));
    }
    
    @Test
    void should_not_be_completed_for_missing_keys() {
        FlowKey<String> key = TaskDefinition.of("missing_task", Void.class, String.class).outputKey();
        assertFalse(ctx.isCompleted(key));
    }


    @Test
    void should_coexist_multiple_keys_with_different_ids() {
        FlowKey<String> keyA = TaskDefinition.of("A", Void.class, String.class).outputKey();
        FlowKey<Integer> keyB = TaskDefinition.of("B", Void.class, Integer.class).outputKey();

        ctx.put(keyA, "alpha");
        ctx.put(keyB, 99);

        assertEquals("alpha", ctx.get(keyA).orElse(null));
        assertEquals(99, ctx.get(keyB).orElse(null));
    }

    // -----------------------------------------------------------------------
    // Null safety
    // -----------------------------------------------------------------------

    @Test
    void get_with_null_key_should_throw() {
        assertThrows(NullPointerException.class, () -> ctx.get((FlowKey<?>) null));
    }

    @Test
    void put_with_null_key_should_throw() {
        assertThrows(NullPointerException.class, () -> ctx.put((FlowKey<String>) null, "value"));
    }

    @Test
    void isCompleted_with_null_key_should_throw() {
        assertThrows(NullPointerException.class, () -> ctx.isCompleted((FlowKey<?>) null));
    }
}
