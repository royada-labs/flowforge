package io.tugrandsolutions.flowforge.spring.autoconfig;

import io.tugrandsolutions.flowforge.spring.bootstrap.TaskScanner;
import io.tugrandsolutions.flowforge.spring.bootstrap.WorkflowPlanRegistrar;
import io.tugrandsolutions.flowforge.spring.registry.DefaultWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.MutableWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class FlowForgeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskHandlerRegistry taskHandlerRegistry() {
        return new TaskHandlerRegistry();
    }

    @Bean
    public TaskScanner taskScanner(TaskHandlerRegistry registry) {
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
}