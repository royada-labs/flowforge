package io.flowforge.spring.bootstrap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.api.FlowForgeClient;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FlowForgeClientIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void should_execute_registered_workflow_by_id() {
        contextRunner.run(ctx -> {
            assertNull(ctx.getStartupFailure());

            FlowForgeClient client = ctx.getBean(FlowForgeClient.class);

            StepVerifier.create(client.execute("apiScore", "hello"))
                    .assertNext(execCtx -> {
                        assertNotNull(execCtx);
                        // ajusta esta assertion a tu API real del ReactiveExecutionContext
                        // Por ejemplo: assertEquals("...", execCtx.get("someKey"))
                    })
                    .verifyComplete();
        });
    }

    @Configuration
    public static class TestConfig {

        @org.springframework.context.annotation.Bean
        public TaskA taskA() {
            return new TaskA();
        }

        @org.springframework.context.annotation.Bean
        public TaskB taskB() {
            return new TaskB();
        }

        @FlowTask(id = "A")
        public static class TaskA implements io.flowforge.api.FlowTaskHandler<Object, Object> {
            @Override
            public Mono<Object> execute(Object input, ReactiveExecutionContext ctx) {
                return Mono.just(input);
            }
        }

        @FlowTask(id = "B")
        public static class TaskB implements io.flowforge.api.FlowTaskHandler<Object, Object> {
            @Override
            public Mono<Object> execute(Object input, ReactiveExecutionContext ctx) {
                return Mono.just("B:" + input);
            }
        }

        static final io.flowforge.task.TaskDefinition<Void, Object> TASK_A = io.flowforge.task.TaskDefinition.of("A", Void.class, Object.class);
        static final io.flowforge.task.TaskDefinition<Object, Object> TASK_B = io.flowforge.task.TaskDefinition.of("B", Object.class, Object.class);

        @org.springframework.context.annotation.Bean
        @FlowWorkflow(id = "apiScore")
        public WorkflowExecutionPlan apiScoreWorkflow(FlowDsl dsl) {
            return dsl.startTyped(TASK_A).then(TASK_B).build();
        }

    }
}
