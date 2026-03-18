package io.flowforge.spring.registry;

import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskDefinitionRegistryTest {

    @Test
    void should_fail_on_conflicting_task_id_registration() {
        TaskDefinitionRegistry registry = new TaskDefinitionRegistry();
        registry.register(TaskDefinition.of(TaskId.of("A"), Void.class, Integer.class), "beanA");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                registry.register(TaskDefinition.of(TaskId.of("A"), Void.class, String.class), "beanB"));

        assertTrue(ex.getMessage().contains("Conflicting TaskDefinition"));
    }

    @Test
    void should_fail_on_conflicting_method_reference_signature_registration() {
        TaskDefinitionRegistry registry = new TaskDefinitionRegistry();
        String implClass = "io/flowforge/TestConfig";
        String methodName = "task";
        String descriptor = "()Lio/flowforge/api/FlowTaskHandler;";

        registry.registerMethodRef(
                implClass,
                methodName,
                descriptor,
                TaskDefinition.of(TaskId.of("X"), Void.class, Integer.class)
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                registry.registerMethodRef(
                        implClass,
                        methodName,
                        descriptor,
                        TaskDefinition.of(TaskId.of("Y"), Void.class, String.class)
                ));

        assertTrue(ex.getMessage().contains("Conflicting TaskDefinition"));
    }
}
