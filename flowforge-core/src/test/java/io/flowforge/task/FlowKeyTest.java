package io.flowforge.task;

import io.flowforge.task.TaskId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FlowKey}.
 */
class FlowKeyTest {

    @Test
    void should_create_key_from_task_definition() {
        FlowKey<String> key = TaskDefinition.of("myTask", Void.class, String.class).outputKey();

        assertEquals("myTask", key.taskId().getValue());
        assertEquals(String.class, key.type());
    }

    @Test
    void should_create_key_with_task_id_and_type() {
        TaskId taskId = TaskId.of("anotherTask");
        FlowKey<Integer> key = new FlowKey<>(taskId, Integer.class);

        assertEquals(taskId, key.taskId());
        assertEquals(Integer.class, key.type());
    }

    @Test
    void keys_with_same_id_and_type_should_be_equal() {
        FlowKey<String> key1 = TaskDefinition.of("task", Void.class, String.class).outputKey();
        FlowKey<String> key2 = TaskDefinition.of("task", Object.class, String.class).outputKey();

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void keys_with_different_types_should_not_be_equal() {
        FlowKey<String> keyString = TaskDefinition.of("task", Void.class, String.class).outputKey();
        FlowKey<Integer> keyInt = TaskDefinition.of("task", Void.class, Integer.class).outputKey();

        assertNotEquals(keyString, keyInt);
    }

    @Test
    void keys_with_different_ids_should_not_be_equal() {
        FlowKey<String> key1 = TaskDefinition.of("task1", Void.class, String.class).outputKey();
        FlowKey<String> key2 = TaskDefinition.of("task2", Void.class, String.class).outputKey();

        assertNotEquals(key1, key2);
    }

    @Test
    void toString_should_include_id_and_type() {
        FlowKey<String> key = TaskDefinition.of("myTask", Void.class, String.class).outputKey();
        String repr = key.toString();

        assertTrue(repr.contains("myTask"), "toString should contain the id");
        assertTrue(repr.contains("String"), "toString should contain the type name");
    }

    @Test
    void should_reject_null_type() {
        assertThrows(NullPointerException.class,
                () -> new FlowKey<>(TaskId.of("task"), null));
    }

    @Test
    void key_should_not_equal_null_or_different_type() {
        FlowKey<String> key = TaskDefinition.of("task", Void.class, String.class).outputKey();

        assertNotEquals(null, key);
        assertNotEquals("not a key", key);
    }
}
