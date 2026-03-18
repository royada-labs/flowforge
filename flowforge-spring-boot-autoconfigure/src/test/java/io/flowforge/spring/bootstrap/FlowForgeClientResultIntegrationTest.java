package io.flowforge.spring.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class FlowForgeClientResultIntegrationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class));

  @Test
  void should_return_single_task_output_when_plan_is_linear() {
    contextRunner.withUserConfiguration(TestConfig.class)
        .run(context -> {
          FlowForgeClient client = context.getBean(FlowForgeClient.class);

          // Workflow "linear": A -> B(terminal, returns "RESULT_B")
          Mono<Object> execution = client.executeResult("linear", null);

          StepVerifier.create(execution)
              .expectNext("RESULT_B")
              .verifyComplete();
        });
  }

  @Test
  void should_return_context_when_plan_has_multiple_terminals() {
    contextRunner.withUserConfiguration(TestConfig.class)
        .run(context -> {
          FlowForgeClient client = context.getBean(FlowForgeClient.class);

          // Workflow "parallel": A -> (B, C) -> B and C are terminals
          Mono<Object> execution = client.executeResult("parallel", null);

          StepVerifier.create(execution)
              .consumeNextWith(result -> {
                assertThat(result).isInstanceOf(ReactiveExecutionContext.class);
                ReactiveExecutionContext ctx = (ReactiveExecutionContext) result;
                assertThat(ctx.isCompleted(TaskDefinition.of("B", Object.class, Object.class).outputKey())).isTrue();
                assertThat(ctx.isCompleted(TaskDefinition.of("C", Object.class, Object.class).outputKey())).isTrue();

              })
              .verifyComplete();
        });
  }

  @Configuration
  static class TestConfig {

    static final TaskDefinition<Void, Object> TASK_A = TaskDefinition.of("A", Void.class, Object.class);
    static final TaskDefinition<Object, Object> TASK_B = TaskDefinition.of("B", Object.class, Object.class);
    static final TaskDefinition<Object, Object> TASK_C = TaskDefinition.of("C", Object.class, Object.class);

    @Bean
    @FlowWorkflow(id = "linear")
    public WorkflowExecutionPlan linearPlan(FlowDsl dsl) {
      // A -> B
      return dsl.startTyped(TASK_A)
          .then(TASK_B)
          .build();
    }


    @Bean
    @FlowWorkflow(id = "parallel")
    public WorkflowExecutionPlan parallelPlan(FlowDsl dsl) {
      // A -> (B, C)
      return dsl.startTyped(TASK_A)
          .fork(
              b -> b.then(TASK_B),
              b -> b.then(TASK_C))
          .build();
    }


    @Bean
    @FlowTask(id = "A")
    public TaskA taskA() {
      return new TaskA();
    }

    @Bean
    @FlowTask(id = "B")
    public TaskB taskB() {
      return new TaskB();
    }

    @Bean
    @FlowTask(id = "C")
    public TaskC taskC() {
      return new TaskC();
    }
  }

  static class TaskA implements FlowTaskHandler<Object, Object> {
    @Override
    public Mono<Object> execute(@Nullable Object input, ReactiveExecutionContext ctx) {
      return Mono.just("RESULT_A");
    }
  }

  static class TaskB implements FlowTaskHandler<Object, Object> {
    @Override
    public Mono<Object> execute(@Nullable Object input, ReactiveExecutionContext ctx) {
      return Mono.just("RESULT_B");
    }
  }

  static class TaskC implements FlowTaskHandler<Object, Object> {
    @Override
    public Mono<Object> execute(@Nullable Object input, ReactiveExecutionContext ctx) {
      return Mono.just("RESULT_C");
    }
  }
}
