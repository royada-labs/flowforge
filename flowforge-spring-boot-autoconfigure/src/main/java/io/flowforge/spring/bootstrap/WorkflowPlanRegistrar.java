package io.flowforge.spring.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringUtils;

import io.flowforge.exception.WorkflowConfigurationException;
import io.flowforge.exception.TaskRegistrationException;
import io.flowforge.registry.WorkflowRegistry;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.spring.workflow.WorkflowDefinition;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Discovers both workflow declaration styles and registers unified descriptors:
 * - method-based (@Bean + @FlowWorkflow returning WorkflowExecutionPlan)
 * - class-based (@FlowWorkflow + WorkflowDefinition)
 *
 * Uses BeanFactoryPostProcessor for metadata discovery and SmartInitializingSingleton
 * for registration to avoid early bean instantiation.
 */
public final class WorkflowPlanRegistrar
        implements BeanFactoryPostProcessor, SmartInitializingSingleton, BeanFactoryAware {

    private ConfigurableListableBeanFactory beanFactory;

    private final WorkflowRegistry workflowRegistry;
    private final Map<String, BeanWorkflowCandidate> beanWorkflowCandidatesById = new LinkedHashMap<>();
    private final Map<String, ClassWorkflowCandidate> classWorkflowCandidatesById = new LinkedHashMap<>();

    public WorkflowPlanRegistrar(WorkflowRegistry workflowRegistry) {
        this.workflowRegistry = workflowRegistry;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new WorkflowConfigurationException("WorkflowPlanRegistrar requires a ConfigurableListableBeanFactory");
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory bf) throws BeansException {
        for (String beanName : bf.getBeanDefinitionNames()) {
            FlowWorkflow ann = bf.findAnnotationOnBean(beanName, FlowWorkflow.class);
            if (ann == null) {
                continue;
            }

            String workflowId = ann.id();
            if (!StringUtils.hasText(workflowId)) {
                throw new WorkflowConfigurationException("@FlowWorkflow id must not be blank. Bean: " + beanName);
            }

            Class<?> beanType = bf.getType(beanName);
            if (beanType == null) {
                throw new WorkflowConfigurationException("Unable to resolve @FlowWorkflow bean type. Bean: " + beanName);
            }

            if (WorkflowExecutionPlan.class.isAssignableFrom(beanType)) {
                registerCandidate(
                        workflowId,
                        new BeanWorkflowCandidate(beanName, resolveBeanSourceClass(beanName, beanType)),
                        beanWorkflowCandidatesById
                );
                continue;
            }

            if (WorkflowDefinition.class.isAssignableFrom(beanType)) {
                registerCandidate(
                        workflowId,
                        new ClassWorkflowCandidate(beanName, beanType),
                        classWorkflowCandidatesById
                );
                continue;
            }

            if (beanType.isAnnotationPresent(FlowWorkflow.class)) {
                throw new WorkflowConfigurationException(
                        "Class-level @FlowWorkflow must implement WorkflowDefinition. Bean: " + beanName
                                + ", type: " + beanType.getName());
            }

            throw new WorkflowConfigurationException(
                    "@FlowWorkflow must annotate either a WorkflowExecutionPlan bean or a WorkflowDefinition class. "
                            + "Bean: " + beanName + ", type: " + beanType.getName());
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (Map.Entry<String, BeanWorkflowCandidate> entry : beanWorkflowCandidatesById.entrySet()) {
            String workflowId = entry.getKey();
            BeanWorkflowCandidate candidate = entry.getValue();

            WorkflowExecutionPlan plan = beanFactory.getBean(candidate.beanName(), WorkflowExecutionPlan.class);
            sanityCheck(plan, workflowId);
            workflowRegistry.register(new BeanWorkflowDescriptor(workflowId, plan, candidate.sourceClass()));
        }

        if (!classWorkflowCandidatesById.isEmpty()) {
            FlowDsl dsl = beanFactory.getBean(FlowDsl.class);
            for (Map.Entry<String, ClassWorkflowCandidate> entry : classWorkflowCandidatesById.entrySet()) {
                String workflowId = entry.getKey();
                ClassWorkflowCandidate candidate = entry.getValue();
                WorkflowDefinition definition = resolveDefinition(candidate);
                WorkflowExecutionPlan plan = definition.define(dsl);
                if (plan == null) {
                    throw new WorkflowConfigurationException("WorkflowDefinition returned null plan. workflowId=" + workflowId
                            + ", source=" + candidate.sourceClass().getName());
                }
                sanityCheck(plan, workflowId);
                workflowRegistry.register(new ClassWorkflowDescriptor(workflowId, plan, candidate.sourceClass()));
            }
        }

        workflowRegistry.seal();
    }

    private Class<?> resolveBeanSourceClass(String beanName, Class<?> fallback) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        String factoryBeanName = beanDefinition.getFactoryBeanName();
        if (factoryBeanName != null) {
            Class<?> factoryType = beanFactory.getType(factoryBeanName);
            if (factoryType != null) {
                return factoryType;
            }
        }
        return fallback;
    }

    private WorkflowDefinition resolveDefinition(ClassWorkflowCandidate candidate) {
        try {
            return beanFactory.getBean(candidate.beanName(), WorkflowDefinition.class);
        } catch (BeansException ex) {
            return instantiateDefinition(candidate, ex);
        }
    }

    private WorkflowDefinition instantiateDefinition(ClassWorkflowCandidate candidate, Exception cause) {
        try {
            Object instance = candidate.sourceClass().getDeclaredConstructor().newInstance();
            if (beanFactory instanceof AutowireCapableBeanFactory autowireCapableBeanFactory) {
                autowireCapableBeanFactory.autowireBean(instance);
            }
            if (!(instance instanceof WorkflowDefinition definition)) {
                throw new WorkflowConfigurationException("Class does not implement WorkflowDefinition: "
                        + candidate.sourceClass().getName());
            }
            return definition;
        } catch (Exception reflectionError) {
            throw new BeanCreationException(
                    "Failed to create WorkflowDefinition for type " + candidate.sourceClass().getName(),
                    cause != null ? cause : reflectionError
            );
        }
    }

    private <T> void registerCandidate(String workflowId, T candidate, Map<String, T> destination) {
        if (beanWorkflowCandidatesById.containsKey(workflowId) || classWorkflowCandidatesById.containsKey(workflowId)) {
            throw new WorkflowConfigurationException("Duplicate workflow id: " + workflowId);
        }
        destination.put(workflowId, candidate);
    }

    private static void sanityCheck(WorkflowExecutionPlan plan, String workflowId) {
        if (plan == null) {
            throw new WorkflowConfigurationException("WorkflowExecutionPlan is null. workflowId=" + workflowId);
        }
        if (plan.nodes().isEmpty()) {
            throw new WorkflowConfigurationException("WorkflowExecutionPlan has no nodes. workflowId=" + workflowId);
        }
        if (plan.roots() == null || plan.roots().isEmpty()) {
            throw new WorkflowConfigurationException("WorkflowExecutionPlan has no roots. workflowId=" + workflowId);
        }
    }

    private record BeanWorkflowCandidate(String beanName, Class<?> sourceClass) {}

    private record ClassWorkflowCandidate(String beanName, Class<?> sourceClass) {}
}