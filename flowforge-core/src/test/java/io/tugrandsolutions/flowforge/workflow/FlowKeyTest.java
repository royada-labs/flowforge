package io.tugrandsolutions.flowforge.workflow;

import io.tugrandsolutions.flowforge.task.TaskId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FlowKey}.
 */
class FlowKeyTest {

    @Test
    void should_create_key_with_string_id_and_type() {
        FlowKey<String> key = FlowKey.of("myTask", String.class);

        assertEquals("myTask", key.taskId().getValue());
        assertEquals(String.class, key.type());
    }

    @Test
    void should_create_key_from_task_id() {
        TaskId taskId = TaskId.of("anotherTask");
        FlowKey<Integer> key = FlowKey.of(taskId, Integer.class);

        assertEquals(taskId, key.taskId());
        assertEquals(Integer.class, key.type());
    }

    @Test
    void keys_with_same_id_and_type_should_be_equal() {
        FlowKey<String> key1 = FlowKey.of("task", String.class);
        FlowKey<String> key2 = FlowKey.of("task", String.class);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void keys_with_different_types_should_not_be_equal() {
        FlowKey<String> keyString = FlowKey.of("task", String.class);
        FlowKey<Integer> keyInt = FlowKey.of("task", Integer.class);

        assertNotEquals(keyString, keyInt);
    }

    @Test
    void keys_with_different_ids_should_not_be_equal() {
        FlowKey<String> key1 = FlowKey.of("task1", String.class);
        FlowKey<String> key2 = FlowKey.of("task2", String.class);

        assertNotEquals(key1, key2);
    }

    @Test
    void toString_should_include_id_and_type() {
        FlowKey<String> key = FlowKey.of("myTask", String.class);
        String repr = key.toString();

        assertTrue(repr.contains("myTask"), "toString should contain the id");
        assertTrue(repr.contains("String"), "toString should contain the type name");
    }

    @Test
    void should_reject_null_id() {
        assertThrows(NullPointerException.class,
                () -> FlowKey.of((String) null, String.class));
    }

    @Test
    void should_reject_blank_id() {
        assertThrows(IllegalArgumentException.class,
                () -> FlowKey.of("  ", String.class));
    }

    @Test
    void should_reject_null_type() {
        assertThrows(NullPointerException.class,
                () -> FlowKey.of("task", null));
    }

    @Test
    void key_should_not_equal_null_or_different_type() {
        FlowKey<String> key = FlowKey.of("task", String.class);

        assertNotEquals(null, key);
        assertNotEquals("not a key", key);
    }
}
