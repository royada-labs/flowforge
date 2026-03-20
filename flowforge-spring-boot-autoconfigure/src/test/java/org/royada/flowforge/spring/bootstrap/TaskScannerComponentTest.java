package org.royada.flowforge.spring.bootstrap;

import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.api.FlowTaskHandler;
import org.royada.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import org.royada.flowforge.spring.registry.TaskHandlerRegistry;
import org.royada.flowforge.task.Task;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class TaskScannerComponentTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(FlowForgeAutoConfiguration.class)
                    );

    @Test
    void should_register_flow_task_component_beans() {
        contextRunner
                .withUserConfiguration(ComponentTasksConfig.class)
                .run(ctx -> {

                    TaskHandlerRegistry registry =
                            ctx.getBean(TaskHandlerRegistry.class);

                    Task<?, ?> task =
                            registry.find(TaskId.of("component-A")).orElseThrow().get();

                    assertEquals(TaskId.of("component-A"), task.id());
                    assertFalse(task.optional());
                    assertTrue(task.dependencies().isEmpty());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ComponentTaskA.class) // IMPORT explícito: no escanea paquetes, no arrastra otros tests
    static class ComponentTasksConfig { }

    @Component
    @FlowTask(id = "component-A")
    static final class ComponentTaskA implements FlowTaskHandler<Void, Integer> {
        @Override
        public Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(1);
        }
    }
}
