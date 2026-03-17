package io.flowforge.task;

import io.flowforge.task.FlowKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskDefinition}.
 */
class TaskDefinitionTest {

    @Test
    void should_create_definition_with_id_and_types() {
        TaskDefinition<String, Integer> def = TaskDefinition.of("transform", String.class, Integer.class);

        assertEquals("transform", def.idValue());
        assertEquals("transform", def.id().getValue());
        assertEquals(String.class, def.inputType());
        assertEquals(Integer.class, def.outputType());
    }

    @Test
    void should_create_from_task_id() {
        TaskId taskId = TaskId.of("myTask");
        TaskDefinition<Void, String> def = TaskDefinition.of(taskId, Void.class, String.class);

        assertEquals(taskId, def.id());
        assertEquals(Void.class, def.inputType());
        assertEquals(String.class, def.outputType());
    }

    @Test
    void definitions_with_same_id_and_types_should_be_equal() {
        TaskDefinition<String, Integer> def1 = TaskDefinition.of("task", String.class, Integer.class);
        TaskDefinition<String, Integer> def2 = TaskDefinition.of("task", String.class, Integer.class);

        assertEquals(def1, def2);
        assertEquals(def1.hashCode(), def2.hashCode());
    }

    @Test
    void definitions_with_different_ids_should_not_be_equal() {
        TaskDefinition<String, Integer> def1 = TaskDefinition.of("task1", String.class, Integer.class);
        TaskDefinition<String, Integer> def2 = TaskDefinition.of("task2", String.class, Integer.class);

        assertNotEquals(def1, def2);
    }

    @Test
    void definitions_with_different_input_types_should_not_be_equal() {
        TaskDefinition<String, Integer> def1 = TaskDefinition.of("task", String.class, Integer.class);
        TaskDefinition<Integer, Integer> def2 = TaskDefinition.of("task", Integer.class, Integer.class);

        assertNotEquals(def1, def2);
    }

    @Test
    void definitions_with_different_output_types_should_not_be_equal() {
        TaskDefinition<String, Integer> def1 = TaskDefinition.of("task", String.class, Integer.class);
        TaskDefinition<String, String> def2 = TaskDefinition.of("task", String.class, String.class);

        assertNotEquals(def1, def2);
    }

    @Test
    void toRef_should_carry_output_type() {
        TaskDefinition<String, Integer> def = TaskDefinition.of("task", String.class, Integer.class);
        TaskRef<Integer> ref = def.toRef();

        assertEquals("task", ref.idValue());
        assertEquals(Integer.class, ref.outputType());
    }

    @Test
    void toKey_should_carry_output_type() {
        TaskDefinition<String, Integer> def = TaskDefinition.of("task", String.class, Integer.class);
        FlowKey<Integer> key = def.toKey();

        assertEquals("task", key.taskId().getValue());
        assertEquals(Integer.class, key.type());
    }

    @Test
    void toString_should_include_id_and_types() {
        TaskDefinition<String, Integer> def = TaskDefinition.of("transform", String.class, Integer.class);
        String repr = def.toString();

        assertTrue(repr.contains("transform"));
        assertTrue(repr.contains("String"));
        assertTrue(repr.contains("Integer"));
    }

    @Test
    void should_reject_null_id() {
        assertThrows(NullPointerException.class,
                () -> TaskDefinition.of((String) null, String.class, Integer.class));
    }

    @Test
    void should_reject_blank_id() {
        assertThrows(IllegalArgumentException.class,
                () -> TaskDefinition.of("  ", String.class, Integer.class));
    }

    @Test
    void should_reject_null_input_type() {
        assertThrows(NullPointerException.class,
                () -> TaskDefinition.of("task", null, Integer.class));
    }

    @Test
    void should_reject_null_output_type() {
        assertThrows(NullPointerException.class,
                () -> TaskDefinition.of("task", String.class, null));
    }

    @Test
    void should_not_equal_null() {
        TaskDefinition<String, Integer> def = TaskDefinition.of("task", String.class, Integer.class);
        assertNotEquals(null, def);
    }
}
