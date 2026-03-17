package io.tugrandsolutions.flowforge.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskRef}.
 */
class TaskRefTest {

    @Test
    void should_create_ref_with_string_id_and_output_type() {
        TaskRef<String> ref = TaskRef.of("fetchUser", String.class);

        assertEquals("fetchUser", ref.idValue());
        assertEquals("fetchUser", ref.id().getValue());
        assertEquals(String.class, ref.outputType());
    }

    @Test
    void should_create_ref_from_task_id() {
        TaskId taskId = TaskId.of("myTask");
        TaskRef<Integer> ref = TaskRef.of(taskId, Integer.class);

        assertEquals(taskId, ref.id());
        assertEquals(Integer.class, ref.outputType());
    }

    @Test
    void refs_with_same_id_and_type_should_be_equal() {
        TaskRef<String> ref1 = TaskRef.of("taskA", String.class);
        TaskRef<String> ref2 = TaskRef.of("taskA", String.class);

        assertEquals(ref1, ref2);
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void refs_with_different_types_should_not_be_equal() {
        TaskRef<String> refString = TaskRef.of("task", String.class);
        TaskRef<Integer> refInt = TaskRef.of("task", Integer.class);

        assertNotEquals(refString, refInt);
    }

    @Test
    void refs_with_different_ids_should_not_be_equal() {
        TaskRef<String> ref1 = TaskRef.of("task1", String.class);
        TaskRef<String> ref2 = TaskRef.of("task2", String.class);

        assertNotEquals(ref1, ref2);
    }

    @Test
    void toString_should_include_id_and_output_type() {
        TaskRef<String> ref = TaskRef.of("fetchUser", String.class);
        String repr = ref.toString();

        assertTrue(repr.contains("fetchUser"), "toString should contain the id");
        assertTrue(repr.contains("String"), "toString should contain the output type name");
    }

    @Test
    void idValue_should_be_consistent_with_id_getValue() {
        TaskRef<String> ref = TaskRef.of("myTask", String.class);

        assertEquals(ref.id().getValue(), ref.idValue());
    }

    @Test
    void should_reject_null_id_string() {
        assertThrows(NullPointerException.class,
                () -> TaskRef.of((String) null, String.class));
    }

    @Test
    void should_reject_blank_id() {
        assertThrows(IllegalArgumentException.class,
                () -> TaskRef.of("", String.class));
    }

    @Test
    void should_reject_blank_id_with_spaces() {
        assertThrows(IllegalArgumentException.class,
                () -> TaskRef.of("   ", String.class));
    }

    @Test
    void should_reject_null_output_type() {
        assertThrows(NullPointerException.class,
                () -> TaskRef.of("task", null));
    }

    @Test
    void ref_should_not_equal_null_or_different_class() {
        TaskRef<String> ref = TaskRef.of("task", String.class);

        assertNotEquals(null, ref);
        assertNotEquals("not a ref", ref);
    }

    @Test
    void toKey_should_produce_flow_key_with_same_id_and_type() {
        TaskRef<String> ref = TaskRef.of("fetchUser", String.class);
        io.tugrandsolutions.flowforge.workflow.FlowKey<String> key = ref.toKey();

        assertEquals("fetchUser", key.taskId().getValue());
        assertEquals(String.class, key.type());
    }
}

