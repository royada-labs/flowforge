package org.royada.flowforge.spring.bootstrap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.royada.flowforge.api.FlowTaskHandler;
import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import org.royada.flowforge.spring.dsl.FlowDsl;
import org.royada.flowforge.spring.workflow.WorkflowDefinition;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import reactor.core.publisher.Mono;

class WorkflowDescriptorValidationTest {

    @Test
    void should_fail_on_duplicate_workflow_ids() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                .withUserConfiguration(DuplicateIdsConfig.class)
                .run(ctx -> {
                    Throwable failure = ctx.getStartupFailure();
                    assertNotNull(failure);
                    assertTrue(failure.getMessage().contains("Duplicate workflow id"));
                });
    }

    @Test
    void should_fail_when_class_workflow_does_not_implement_definition() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlowForgeAutoConfiguration.class))
                .withUserConfiguration(InvalidClassWorkflowConfig.class)
                .run(ctx -> {
                    Throwable failure = ctx.getStartupFailure();
                    assertNotNull(failure);
                    assertTrue(failure.getMessage().contains("must implement WorkflowDefinition"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateIdsConfig {

        @Bean
        StartTask startTask() {
            return new StartTask();
        }

        @Bean
        @FlowWorkflow(id = "dup-flow")
        WorkflowExecutionPlan legacy(FlowDsl dsl) {
            return dsl.startTyped(TaskDefinition.of("dup-start", Void.class, Integer.class)).build();
        }

        @Bean
        DuplicateClassFlow duplicateClassFlow() {
            return new DuplicateClassFlow();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class InvalidClassWorkflowConfig {

        @Bean
        @FlowWorkflow(id = "bad-flow")
        BadWorkflowType badWorkflowType() {
            return new BadWorkflowType();
        }
    }

    @FlowWorkflow(id = "dup-flow")
    static class DuplicateClassFlow implements WorkflowDefinition {
        @Override
        public WorkflowExecutionPlan define(FlowDsl dsl) {
            return dsl.startTyped(TaskDefinition.of("dup-start", Void.class, Integer.class)).build();
        }
    }

    @FlowWorkflow(id = "bad-flow")
    static class BadWorkflowType {
    }

    @FlowTask(id = "dup-start")
    static class StartTask implements FlowTaskHandler<Void, Integer> {
        @Override
        public Mono<Integer> execute(Void input, ReactiveExecutionContext ctx) {
            return Mono.just(1);
        }
    }
}
