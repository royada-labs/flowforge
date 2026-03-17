package io.flowforge.dsl;


import io.flowforge.task.TaskRef;
import io.flowforge.task.FlowKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypedTaskNode}.
 */
class TypedTaskNodeTest {

    @Test
    void should_create_node_from_task_ref() {
        TaskRef<String> ref = TaskRef.of("myTask", String.class);
        TypedTaskNode<String> node = new TypedTaskNode<>(ref);

        assertSame(ref, node.ref());
    }

    @Test
    void toKey_should_delegate_to_ref() {
        TaskRef<Integer> ref = TaskRef.of("calc", Integer.class);
        TypedTaskNode<Integer> node = new TypedTaskNode<>(ref);

        FlowKey<Integer> key = node.toKey();

        assertEquals("calc", key.taskId().getValue());
        assertEquals(Integer.class, key.type());
    }

    @Test
    void nodes_with_same_ref_should_be_equal() {
        TaskRef<String> ref1 = TaskRef.of("task", String.class);
        TaskRef<String> ref2 = TaskRef.of("task", String.class);

        TypedTaskNode<String> node1 = new TypedTaskNode<>(ref1);
        TypedTaskNode<String> node2 = new TypedTaskNode<>(ref2);

        assertEquals(node1, node2);
        assertEquals(node1.hashCode(), node2.hashCode());
    }

    @Test
    void nodes_with_different_refs_should_not_be_equal() {
        TypedTaskNode<String> node1 = new TypedTaskNode<>(TaskRef.of("task1", String.class));
        TypedTaskNode<String> node2 = new TypedTaskNode<>(TaskRef.of("task2", String.class));

        assertNotEquals(node1, node2);
    }

    @Test
    void toString_should_include_ref_info() {
        TypedTaskNode<String> node = new TypedTaskNode<>(TaskRef.of("myTask", String.class));
        String repr = node.toString();

        assertTrue(repr.contains("myTask"));
        assertTrue(repr.contains("String"));
    }

    @Test
    void should_reject_null_ref() {
        assertThrows(NullPointerException.class, () -> new TypedTaskNode<>(null));
    }

    @Test
    void should_not_equal_null() {
        TypedTaskNode<String> node = new TypedTaskNode<>(TaskRef.of("task", String.class));
        assertNotEquals(null, node);
    }
}
