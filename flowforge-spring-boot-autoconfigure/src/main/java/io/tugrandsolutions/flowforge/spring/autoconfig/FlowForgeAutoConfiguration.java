package io.tugrandsolutions.flowforge.spring.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import io.tugrandsolutions.flowforge.spring.api.FlowForgeClient;
import io.tugrandsolutions.flowforge.spring.bootstrap.TaskScanner;
import io.tugrandsolutions.flowforge.spring.bootstrap.WorkflowPlanRegistrar;
import io.tugrandsolutions.flowforge.spring.impl.DefaultFlowForgeClient;
import io.tugrandsolutions.flowforge.spring.registry.DefaultWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.MutableWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.spring.registry.WorkflowPlanRegistry;

@AutoConfiguration
public class FlowForgeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskHandlerRegistry taskHandlerRegistry() {
        return new TaskHandlerRegistry();
    }

    @Bean
    static TaskScanner taskScanner(TaskHandlerRegistry registry) {
        return new TaskScanner(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public MutableWorkflowPlanRegistry workflowPlanRegistry() {
        return new DefaultWorkflowPlanRegistry();
    }

    @Bean
    public WorkflowPlanRegistrar workflowPlanRegistrar(MutableWorkflowPlanRegistry registry) {
        return new WorkflowPlanRegistrar(registry);
    }

    @Bean
    public io.tugrandsolutions.flowforge.spring.dsl.FlowDsl flowDsl(
            io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry taskRegistry) {
        return new io.tugrandsolutions.flowforge.spring.dsl.DefaultFlowDsl(taskRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator reactiveWorkflowOrchestrator() {
        return new io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator();
    }

    @Bean
    FlowForgeClient flowForgeClient(
            WorkflowPlanRegistry workflowPlanRegistry,
            io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator orchestrator) {
        return new DefaultFlowForgeClient(workflowPlanRegistry, orchestrator);
    }
}