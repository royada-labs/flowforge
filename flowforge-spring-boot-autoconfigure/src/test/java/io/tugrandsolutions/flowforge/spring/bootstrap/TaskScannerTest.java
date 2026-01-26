package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.api.FlowTaskHandler;
import io.tugrandsolutions.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.ReactiveExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import io.tugrandsolutions.flowforge.task.Task;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TaskScannerTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(FlowForgeAutoConfiguration.class)
                    );

    @Test
    void should_register_flow_tasks_annotated_beans() {
        contextRunner
                .withUserConfiguration(ValidTasksConfig.class)
                .run(ctx -> {

                    TaskHandlerRegistry registry =
                            ctx.getBean(TaskHandlerRegistry.class);

                    io.tugrandsolutions.flowforge.task.Task<?, ?> taskA =
                            registry.find(new TaskId("A")).orElseThrow();
                    Task<?, ?> taskB =
                            registry.find(new TaskId("B")).orElseThrow();

                    assertFalse(taskA.optional());
                    assertEquals(Set.of(), taskA.dependencies());

                    assertTrue(taskB.optional());
                    assertEquals(
                            Set.of(new TaskId("A")),
                            taskB.dependencies()
                    );
                });
    }

    @Test
    void should_fail_on_duplicate_task_id() {
        contextRunner
                .withUserConfiguration(DuplicateTaskConfig.class)
                .run(ctx -> {
                    Throwable failure = ctx.getStartupFailure();
                    assertNotNull(failure, "Context should fail on duplicate task id");
                    assertTrue(
                            failure.getMessage().contains("Duplicate FlowTask id"),
                            failure.getMessage()
                    );
                });
    }

    @Test
    void should_fail_when_annotated_bean_does_not_implement_handler() {
        contextRunner
                .withUserConfiguration(InvalidTaskConfig.class)
                .run(ctx -> {
                    Throwable failure = ctx.getStartupFailure();
                    assertNotNull(
                            failure,
                            "Context should fail when @FlowTask bean is not a handler"
                    );
                    assertTrue(
                            failure.getMessage().contains("implements FlowTaskHandler"),
                            failure.getMessage()
                    );
                });
    }

    // ---------------------------------------------------------------------
    // Test configurations
    // ---------------------------------------------------------------------

    @Configuration(proxyBeanMethods = false)
    static class ValidTasksConfig {

        @Bean
        @FlowTask(id = "A")
        TaskA taskA() {
            return new TaskA();
        }

        @Bean
        @FlowTask(
                id = "B",
                optional = true,
                dependsOn = {"A"}
        )
        TaskB taskB() {
            return new TaskB();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateTaskConfig {

        @Bean
        @FlowTask(id = "A")
        TaskA taskA1() {
            return new TaskA();
        }

        @Bean
        @FlowTask(id = "A")
        TaskA taskA2() {
            return new TaskA();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class InvalidTaskConfig {

        @Bean
        @FlowTask(id = "X")
        NotAHandler notAHandler() {
            return new NotAHandler();
        }
    }

    // ---------------------------------------------------------------------
    // Test task implementations
    // ---------------------------------------------------------------------

    static final class TaskA
            implements FlowTaskHandler<Void, Integer> {

        @Override
        public Mono<Integer> execute(
                Void input,
                ReactiveExecutionContext ctx
        ) {
            return Mono.just(1);
        }
    }

    static final class TaskB
            implements FlowTaskHandler<Integer, Integer> {

        @Override
        public Mono<Integer> execute(
                Integer input,
                ReactiveExecutionContext ctx
        ) {
            return Mono.just(input + 1);
        }
    }

    static final class NotAHandler {
        // intentionally empty
    }
}