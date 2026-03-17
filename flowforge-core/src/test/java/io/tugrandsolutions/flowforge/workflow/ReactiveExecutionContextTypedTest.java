package io.tugrandsolutions.flowforge.workflow;

import io.tugrandsolutions.flowforge.task.TaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
        FlowKey<String> key = FlowKey.of("taskA", String.class);

        ctx.put(key, "hello");

        Optional<String> result = ctx.get(key);
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    void should_put_and_get_integer_with_flow_key() {
        FlowKey<Integer> key = FlowKey.of("taskB", Integer.class);

        ctx.put(key, 42);

        Optional<Integer> result = ctx.get(key);
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    void should_return_empty_optional_when_key_not_present() {
        FlowKey<String> key = FlowKey.of("nonExistent", String.class);

        Optional<String> result = ctx.get(key);

        assertFalse(result.isPresent());
    }

    @Test
    void should_return_empty_when_stored_type_does_not_match() {
        // Store as raw TaskId
        TaskId id = TaskId.of("typeMismatch");
        ctx.put(id, 42); // Integer stored

        FlowKey<String> stringKey = FlowKey.of(id, String.class);
        // The underlying get(TaskId, Class) uses filter(type::isInstance) — returns empty on mismatch
        Optional<String> result = ctx.get(stringKey);

        assertFalse(result.isPresent(),
                "Should return empty when stored Integer is fetched as String");
    }

    // -----------------------------------------------------------------------
    // isCompleted with FlowKey
    // -----------------------------------------------------------------------

    @Test
    void should_report_completed_after_put_via_flow_key() {
        FlowKey<String> key = FlowKey.of("taskC", String.class);

        assertFalse(ctx.isCompleted(key));

        ctx.put(key, "done");

        assertTrue(ctx.isCompleted(key));
    }

    // -----------------------------------------------------------------------
    // Coexistence: FlowKey API and legacy TaskId API on the same store
    // -----------------------------------------------------------------------

    @Test
    void flow_key_put_should_be_visible_via_legacy_task_id_get() {
        FlowKey<String> key = FlowKey.of("shared", String.class);
        TaskId taskId = key.taskId();

        ctx.put(key, "value");

        // Readable via legacy API
        Optional<String> legacy = ctx.get(taskId, String.class);
        assertTrue(legacy.isPresent());
        assertEquals("value", legacy.get());
    }

    @Test
    void legacy_task_id_put_should_be_visible_via_flow_key_get() {
        TaskId taskId = TaskId.of("shared2");
        FlowKey<String> key = FlowKey.of(taskId, String.class);

        ctx.put(taskId, "legacyValue");

        // Readable via typed API
        Optional<String> typed = ctx.get(key);
        assertTrue(typed.isPresent());
        assertEquals("legacyValue", typed.get());
    }

    @Test
    void should_coexist_multiple_keys_with_different_ids() {
        FlowKey<String> keyA = FlowKey.of("A", String.class);
        FlowKey<Integer> keyB = FlowKey.of("B", Integer.class);

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
