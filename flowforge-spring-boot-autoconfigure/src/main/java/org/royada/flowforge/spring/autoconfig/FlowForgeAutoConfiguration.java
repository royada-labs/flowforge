package org.royada.flowforge.spring.autoconfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import org.royada.flowforge.api.FlowForgeClient;
import org.royada.flowforge.impl.DefaultFlowForgeClient;
import org.royada.flowforge.registry.WorkflowPlanRegistry;
import org.royada.flowforge.registry.WorkflowRegistry;
import org.royada.flowforge.spring.bootstrap.TaskScanner;
import org.royada.flowforge.spring.bootstrap.WorkflowPlanRegistrar;
import org.royada.flowforge.spring.dsl.DefaultFlowDsl;
import org.royada.flowforge.spring.dsl.FlowDsl;
import org.royada.flowforge.spring.registry.DefaultWorkflowPlanRegistry;
import org.royada.flowforge.spring.registry.MutableWorkflowPlanRegistry;
import org.royada.flowforge.spring.registry.TaskDefinitionRegistry;
import org.royada.flowforge.spring.registry.TaskHandlerRegistry;
import org.royada.flowforge.workflow.input.DefaultTaskInputResolver;
import org.royada.flowforge.workflow.monitor.AsyncLoggingWorkflowMonitor;
import org.royada.flowforge.workflow.monitor.NoOpWorkflowMonitor;
import org.royada.flowforge.workflow.monitor.WorkflowMonitor;
import org.royada.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import reactor.core.scheduler.Schedulers;

/**
 * Core Spring Boot auto-configuration for FlowForge runtime components.
 */
@AutoConfiguration
@org.springframework.boot.context.properties.EnableConfigurationProperties(FlowForgeProperties.class)
public class FlowForgeAutoConfiguration {

    /**
     * Creates auto-configuration instance.
     */
    public FlowForgeAutoConfiguration() {
    }

    /**
     * Creates the registry that stores executable task providers.
     *
     * @return task handler registry
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskHandlerRegistry taskHandlerRegistry() {
        return new TaskHandlerRegistry();
    }

    /**
     * Creates the registry used for typed task metadata and method-reference resolution.
     *
     * @return task definition registry
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskDefinitionRegistry taskDefinitionRegistry() {
        return new TaskDefinitionRegistry();
    }

    /**
     * Registers the bean-factory post-processor that discovers {@code @FlowTask} declarations.
     *
     * @param registry task handler registry
     * @param definitionRegistry task definition registry
     * @return task scanner
     */
    @Bean
    static TaskScanner taskScanner(TaskHandlerRegistry registry, TaskDefinitionRegistry definitionRegistry) {
        return new TaskScanner(registry, definitionRegistry);
    }

    /**
     * Creates the mutable workflow plan registry used internally by auto-configuration.
     *
     * @return default workflow plan registry
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultWorkflowPlanRegistry workflowPlanRegistry() {
        return new DefaultWorkflowPlanRegistry();
    }

    /**
     * Exposes mutable workflow registry operations as a dedicated interface.
     *
     * @param registry backing registry
     * @return mutable workflow registry
     */
    @Bean
    @ConditionalOnMissingBean
    public MutableWorkflowPlanRegistry mutableWorkflowPlanRegistry(DefaultWorkflowPlanRegistry registry) {
        return registry;
    }

    /**
     * Exposes workflow plan lookup operations.
     *
     * @param registry backing registry
     * @return read-oriented plan registry view
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowPlanRegistry workflowPlanRegistryView(DefaultWorkflowPlanRegistry registry) {
        return registry;
    }

    /**
     * Exposes workflow descriptors and plan retrieval APIs.
     *
     * @param registry backing registry
     * @return workflow registry
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowRegistry workflowRegistry(DefaultWorkflowPlanRegistry registry) {
        return registry;
    }

    /**
     * Registers startup-time workflow discovery and sealing.
     *
     * @param registry workflow registry
     * @return workflow registrar
     */
    @Bean
    public WorkflowPlanRegistrar workflowPlanRegistrar(WorkflowRegistry registry) {
        return new WorkflowPlanRegistrar(registry);
    }

    /**
     * Provides the typed Flow DSL entry point.
     *
     * @param taskRegistry task provider registry
     * @param definitionRegistry task definition registry
     * @return Flow DSL implementation
     */
    @Bean
    public FlowDsl flowDsl(TaskHandlerRegistry taskRegistry, TaskDefinitionRegistry definitionRegistry) {
        return new DefaultFlowDsl(taskRegistry, definitionRegistry);
    }

    /**
     * Provides the default async logging monitor when monitoring is enabled.
     *
     * @return workflow monitor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "flowforge.monitor.enabled", havingValue = "true", matchIfMissing = true)
    public WorkflowMonitor loggingWorkflowMonitor() {
        return new AsyncLoggingWorkflowMonitor();
    }

    /**
     * Creates the shared state scheduler used by FlowForge orchestration.
     *
     * @return single-thread scheduler
     */
    @Bean(destroyMethod = "dispose")
    @ConditionalOnMissingBean(name = "flowForgeScheduler")
    public reactor.core.scheduler.Scheduler flowForgeScheduler() {
        return Schedulers.newSingle("flowforge");
    }

    /**
     * Creates the reactive orchestrator used to execute workflow plans.
     *
     * @param monitorProvider optional monitor provider
     * @param tracerFactoryProvider optional tracer factory provider
     * @param flowForgeScheduler scheduler used for orchestrator state transitions
     * @param properties flowforge runtime properties
     * @return configured orchestrator
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveWorkflowOrchestrator reactiveWorkflowOrchestrator(
            ObjectProvider<WorkflowMonitor> monitorProvider,
            ObjectProvider<org.royada.flowforge.workflow.trace.ExecutionTracerFactory> tracerFactoryProvider,
            reactor.core.scheduler.Scheduler flowForgeScheduler,
            FlowForgeProperties properties) {
        WorkflowMonitor monitor = monitorProvider.getIfAvailable(NoOpWorkflowMonitor::new);
        org.royada.flowforge.workflow.trace.ExecutionTracerFactory tracerFactory = tracerFactoryProvider.getIfAvailable();

        org.royada.flowforge.workflow.orchestrator.ExecutionLimits limits = 
            new org.royada.flowforge.workflow.orchestrator.ExecutionLimits(
                properties.getExecution().getMaxInFlightTasks(),
                properties.getExecution().getMaxQueueSize(),
                properties.getExecution().getBackpressureStrategy()
            );

        return ReactiveWorkflowOrchestrator.builder()
                .taskScheduler(Schedulers.boundedElastic())
                .stateScheduler(flowForgeScheduler)
                .monitor(monitor)
                .inputResolver(new DefaultTaskInputResolver())
                .tracerFactory(tracerFactory != null ? tracerFactory : types -> new org.royada.flowforge.workflow.trace.NoOpExecutionTracer())
                .limits(limits)
                .build();
    }

    /**
     * Creates the FlowForge client facade.
     *
     * @param workflowRegistry workflow registry
     * @param orchestrator orchestrator implementation
     * @return flowforge client
     */
    @Bean
    FlowForgeClient flowForgeClient(
            WorkflowRegistry workflowRegistry,
            ReactiveWorkflowOrchestrator orchestrator) {
        return new DefaultFlowForgeClient(workflowRegistry, orchestrator);
    }
}
