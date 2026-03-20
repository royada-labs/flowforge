package org.royada.flowforge.workflow;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.royada.flowforge.task.FlowKey;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.exception.TypeMismatchException;

class ReactiveExecutionContextTypedEdgeCasesTest {

    private ReactiveExecutionContext context;

    @BeforeEach
    void setUp() {
        context = new InMemoryReactiveExecutionContext();
    }

    @Test
    void should_handle_primitive_wrapper_types() {
        FlowKey<Integer> intKey = TaskDefinition.of("intTask", Void.class, Integer.class).outputKey();
        
        context.put(intKey, Integer.valueOf(42));
        assertEquals(42, context.get(intKey).orElse(null));
        
        FlowKey<Double> doubleKey = TaskDefinition.of("doubleTask", Void.class, Double.class).outputKey();
        
        context.put(doubleKey, Double.valueOf(3.14));
        assertEquals(3.14, context.get(doubleKey).orElse(null));
    }

    @Test
    void should_maintain_type_safety_across_multiple_puts() {
        FlowKey<String> keyA = TaskDefinition.of("taskA", Void.class, String.class).outputKey();
        FlowKey<Integer> keyB = TaskDefinition.of("taskB", Void.class, Integer.class).outputKey();
        FlowKey<Boolean> keyC = TaskDefinition.of("taskC", Void.class, Boolean.class).outputKey();
        
        context.put(keyA, "hello");
        context.put(keyB, 100);
        context.put(keyC, true);
        
        assertEquals("hello", context.get(keyA).orElse(null));
        assertEquals(100, context.get(keyB).orElse(null));
        assertEquals(true, context.get(keyC).orElse(null));
        
        context.put(keyA, "updated");
        context.put(keyB, 200);
        context.put(keyC, false);
        
        assertEquals("updated", context.get(keyA).orElse(null));
        assertEquals(200, context.get(keyB).orElse(null));
        assertEquals(false, context.get(keyC).orElse(null));
    }

    @Test
    void isCompleted_should_track_multiple_keys() {
        FlowKey<String> keyA = TaskDefinition.of("taskA", Void.class, String.class).outputKey();
        FlowKey<Integer> keyB = TaskDefinition.of("taskB", Void.class, Integer.class).outputKey();
        FlowKey<Boolean> keyC = TaskDefinition.of("taskC", Void.class, Boolean.class).outputKey();
        
        assertFalse(context.isCompleted(keyA));
        assertFalse(context.isCompleted(keyB));
        assertFalse(context.isCompleted(keyC));
        
        context.put(keyA, "valueA");
        assertTrue(context.isCompleted(keyA));
        assertFalse(context.isCompleted(keyB));
        assertFalse(context.isCompleted(keyC));
        
        context.put(keyB, 42);
        assertTrue(context.isCompleted(keyA));
        assertTrue(context.isCompleted(keyB));
        assertFalse(context.isCompleted(keyC));
        
        context.put(keyC, true);
        assertTrue(context.isCompleted(keyA));
        assertTrue(context.isCompleted(keyB));
        assertTrue(context.isCompleted(keyC));
    }

    @Test
    void should_handle_empty_optional_when_key_not_present() {
        FlowKey<String> key = TaskDefinition.of("missing", Void.class, String.class).outputKey();
        
        assertTrue(context.get(key).isEmpty());
    }

    @Test
    void should_throw_on_getOrThrow_when_key_not_present() {
        FlowKey<String> key = TaskDefinition.of("missing", Void.class, String.class).outputKey();
        
        assertThrows(NoSuchElementException.class, () -> {
            context.getOrThrow(key);
        });
    }

    @Test
    void getOrDefault_should_return_default_when_missing() {
        FlowKey<String> key = TaskDefinition.of("missing", Void.class, String.class).outputKey();
        
        String result = context.getOrDefault(key, "default");
        assertEquals("default", result);
    }

    @Test
    void getOrDefault_should_return_value_when_present() {
        FlowKey<String> key = TaskDefinition.of("present", Void.class, String.class).outputKey();
        context.put(key, "actual");
        
        String result = context.getOrDefault(key, "default");
        assertEquals("actual", result);
    }

    @Test
    void should_reject_type_mismatch_when_taskid_already_has_different_type() {
        FlowKey<String> stringKey = TaskDefinition.of("task", Void.class, String.class).outputKey();
        context.put(stringKey, "hello");
        
        FlowKey<Integer> intKey = TaskDefinition.of("task", Void.class, Integer.class).outputKey();
        
        TypeMismatchException ex = assertThrows(TypeMismatchException.class, () -> {
            context.get(intKey);
        });
        
        assertEquals(String.class, ex.getActualType());
        assertEquals(Integer.class, ex.getExpectedType());
    }
}
