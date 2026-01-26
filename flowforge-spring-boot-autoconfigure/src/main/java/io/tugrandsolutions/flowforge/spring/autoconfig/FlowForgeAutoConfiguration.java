package io.tugrandsolutions.flowforge.spring.autoconfig;

import io.tugrandsolutions.flowforge.spring.dsl.DefaultFlowDsl;
import io.tugrandsolutions.flowforge.spring.dsl.FlowDsl;
import io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.tugrandsolutions.flowforge.api.FlowForgeClient;
import io.tugrandsolutions.flowforge.spring.bootstrap.TaskScanner;
import io.tugrandsolutions.flowforge.spring.bootstrap.WorkflowPlanRegistrar;
import io.tugrandsolutions.flowforge.impl.DefaultFlowForgeClient;
import io.tugrandsolutions.flowforge.spring.registry.DefaultWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.MutableWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.TaskHandlerRegistry;
import io.tugrandsolutions.flowforge.registry.WorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.workflow.input.DefaultTaskInputResolver;
import io.tugrandsolutions.flowforge.workflow.monitor.AsyncLoggingWorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.tugrandsolutions.flowforge.workflow.monitor.WorkflowMonitor;
import reactor.core.scheduler.Schedulers;

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
    public FlowDsl flowDsl(TaskHandlerRegistry taskRegistry) {
        return new DefaultFlowDsl(taskRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "flowforge.monitor.enabled", havingValue = "true", matchIfMissing = true)
    public WorkflowMonitor loggingWorkflowMonitor() {
        return new AsyncLoggingWorkflowMonitor();
    }

    @Bean(destroyMethod = "dispose")
    @ConditionalOnMissingBean(name = "flowForgeScheduler")
    public reactor.core.scheduler.Scheduler flowForgeScheduler() {
        return Schedulers.newSingle("flowforge");
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveWorkflowOrchestrator reactiveWorkflowOrchestrator(
            ObjectProvider<WorkflowMonitor> monitorProvider,
            reactor.core.scheduler.Scheduler flowForgeScheduler) {
        WorkflowMonitor monitor = monitorProvider.getIfAvailable(NoOpWorkflowMonitor::new);

        return new ReactiveWorkflowOrchestrator(
                Schedulers.boundedElastic(), // Tasks use global elastic
                flowForgeScheduler, // State uses dedicated single thread
                monitor,
                new DefaultTaskInputResolver(),
                Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    @Bean
    FlowForgeClient flowForgeClient(
            WorkflowPlanRegistry workflowPlanRegistry,
            ReactiveWorkflowOrchestrator orchestrator) {
        return new DefaultFlowForgeClient(workflowPlanRegistry, orchestrator);
    }
}