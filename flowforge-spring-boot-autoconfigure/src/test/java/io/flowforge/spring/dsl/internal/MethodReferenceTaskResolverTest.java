package io.flowforge.spring.dsl.internal;

import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.dsl.TaskMethodRef;
import io.flowforge.spring.registry.TaskDefinitionRegistry;
import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodReferenceTaskResolverTest {

    @Test
    void should_not_fallback_to_bean_name_when_signature_is_not_registered() {
        TaskDefinitionRegistry registry = new TaskDefinitionRegistry();
        registry.register(TaskDefinition.of(TaskId.of("producer"), Void.class, Integer.class), "producer");

        MethodReferenceTaskResolver resolver = new MethodReferenceTaskResolver(registry);
        TaskMethodRef<LocalConfig, Void, Integer> ref = LocalConfig::producer;

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resolver.resolve(ref));
        assertTrue(ex.getMessage().contains("No @FlowTask metadata found"));
    }

    static class LocalConfig {
        FlowTaskHandler<Void, Integer> producer() {
            return (input, ctx) -> Mono.just(1);
        }
    }
}
