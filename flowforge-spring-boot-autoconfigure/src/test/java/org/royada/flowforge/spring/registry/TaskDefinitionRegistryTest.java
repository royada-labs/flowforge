package org.royada.flowforge.spring.registry;

import org.royada.flowforge.exception.TaskRegistrationException;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.task.TaskId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskDefinitionRegistryTest {

    @Test
    void should_fail_on_conflicting_task_id_registration() {
        TaskDefinitionRegistry registry = new TaskDefinitionRegistry();
        registry.register(TaskDefinition.of(TaskId.of("A"), Void.class, Integer.class), "beanA");

        TaskRegistrationException ex = assertThrows(TaskRegistrationException.class, () ->
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

        TaskRegistrationException ex = assertThrows(TaskRegistrationException.class, () ->
                registry.registerMethodRef(
                        implClass,
                        methodName,
                        descriptor,
                        TaskDefinition.of(TaskId.of("Y"), Void.class, String.class)
                ));

        assertTrue(ex.getMessage().contains("Conflicting TaskDefinition"));
    }
}
