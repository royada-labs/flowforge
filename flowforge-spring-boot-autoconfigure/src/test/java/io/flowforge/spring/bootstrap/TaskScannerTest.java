package io.flowforge.spring.bootstrap;

import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import io.flowforge.task.Task;

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

                    io.flowforge.task.Task<?, ?> taskA =
                            registry.find(TaskId.of("A")).orElseThrow().get();
                    Task<?, ?> taskB =
                            registry.find(TaskId.of("B")).orElseThrow().get();

                    assertFalse(taskA.optional());
                    assertEquals(Set.of(), taskA.dependencies());

                    assertTrue(taskB.optional());
                    assertEquals(
                            Set.of(TaskId.of("A")),
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
                            failure.getMessage().contains("implement FlowTaskHandler"),
                            failure.getMessage()
                    );
                });
    }

    @Test
    void should_fail_when_annotation_and_inferred_types_mismatch() {
        contextRunner
                .withUserConfiguration(TypeMismatchTaskConfig.class)
                .run(ctx -> {
                    Throwable failure = ctx.getStartupFailure();
                    assertNotNull(failure, "Context should fail on type mismatch");
                    assertTrue(
                            failure.getMessage().contains("type mismatch"),
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

    @Configuration(proxyBeanMethods = false)
    static class TypeMismatchTaskConfig {
        @Bean
        @FlowTask(id = "M", inputType = String.class, outputType = Integer.class)
        FlowTaskHandler<Integer, Integer> mismatched() {
            return (input, ctx) -> Mono.just(input == null ? 0 : input + 1);
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
