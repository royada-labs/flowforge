package io.flowforge.spring.autoconfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.impl.DefaultFlowForgeClient;
import io.flowforge.registry.WorkflowPlanRegistry;
import io.flowforge.registry.WorkflowRegistry;
import io.flowforge.spring.bootstrap.TaskScanner;
import io.flowforge.spring.bootstrap.WorkflowPlanRegistrar;
import io.flowforge.spring.dsl.DefaultFlowDsl;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.spring.registry.DefaultWorkflowPlanRegistry;
import io.flowforge.spring.registry.MutableWorkflowPlanRegistry;
import io.flowforge.spring.registry.TaskDefinitionRegistry;
import io.flowforge.spring.registry.TaskHandlerRegistry;
import io.flowforge.workflow.input.DefaultTaskInputResolver;
import io.flowforge.workflow.monitor.AsyncLoggingWorkflowMonitor;
import io.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import io.flowforge.workflow.monitor.WorkflowMonitor;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import reactor.core.scheduler.Schedulers;

@AutoConfiguration
@org.springframework.boot.context.properties.EnableConfigurationProperties(FlowForgeProperties.class)
public class FlowForgeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskHandlerRegistry taskHandlerRegistry() {
        return new TaskHandlerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskDefinitionRegistry taskDefinitionRegistry() {
        return new TaskDefinitionRegistry();
    }

    @Bean
    static TaskScanner taskScanner(TaskHandlerRegistry registry, TaskDefinitionRegistry definitionRegistry) {
        return new TaskScanner(registry, definitionRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultWorkflowPlanRegistry workflowPlanRegistry() {
        return new DefaultWorkflowPlanRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public MutableWorkflowPlanRegistry mutableWorkflowPlanRegistry(DefaultWorkflowPlanRegistry registry) {
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowPlanRegistry workflowPlanRegistryView(DefaultWorkflowPlanRegistry registry) {
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowRegistry workflowRegistry(DefaultWorkflowPlanRegistry registry) {
        return registry;
    }

    @Bean
    public WorkflowPlanRegistrar workflowPlanRegistrar(WorkflowRegistry registry) {
        return new WorkflowPlanRegistrar(registry);
    }

    @Bean
    public FlowDsl flowDsl(TaskHandlerRegistry taskRegistry, TaskDefinitionRegistry definitionRegistry) {
        return new DefaultFlowDsl(taskRegistry, definitionRegistry);
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
            ObjectProvider<io.flowforge.workflow.trace.ExecutionTracerFactory> tracerFactoryProvider,
            reactor.core.scheduler.Scheduler flowForgeScheduler,
            FlowForgeProperties properties) {
        WorkflowMonitor monitor = monitorProvider.getIfAvailable(NoOpWorkflowMonitor::new);
        io.flowforge.workflow.trace.ExecutionTracerFactory tracerFactory = tracerFactoryProvider.getIfAvailable();

        io.flowforge.workflow.orchestrator.ExecutionLimits limits = 
            new io.flowforge.workflow.orchestrator.ExecutionLimits(
                properties.getExecution().getMaxInFlightTasks(),
                properties.getExecution().getMaxQueueSize(),
                properties.getExecution().getBackpressureStrategy()
            );

        return new ReactiveWorkflowOrchestrator(
                Schedulers.boundedElastic(),
                flowForgeScheduler,
                monitor,
                new DefaultTaskInputResolver(),
                tracerFactory,
                limits);
    }

    @Bean
    FlowForgeClient flowForgeClient(
            WorkflowRegistry workflowRegistry,
            ReactiveWorkflowOrchestrator orchestrator) {
        return new DefaultFlowForgeClient(workflowRegistry, orchestrator);
    }
}
