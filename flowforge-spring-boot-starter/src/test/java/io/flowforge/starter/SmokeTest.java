package io.flowforge.starter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.api.FlowForgeClient;
import io.flowforge.api.FlowTaskHandler;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(classes = SmokeTest.TestApp.class)
class SmokeTest {

  @Autowired
  private FlowForgeClient client;

  @Test
  void contextLoadsAndWorkflowExecutes() {
    assertThat(client).isNotNull();

    StepVerifier.create(client.execute("smokeWorkflow", "test"))
        .expectNextCount(1)
        .verifyComplete();
  }

  @SpringBootApplication
  static class TestApp {

    @Component
    @FlowTask(id = "smokeTask")
    static class SmokeTask implements FlowTaskHandler<Object, Object> {
      @Override
      public Mono<Object> execute(Object input, ReactiveExecutionContext ctx) {
        return Mono.just("processed:" + input);
      }
    }

    @Bean
    @FlowWorkflow(id = "smokeWorkflow")
    WorkflowExecutionPlan smokeWorkflow(FlowDsl dsl) {
      return dsl.start("smokeTask").build();
    }
  }
}
