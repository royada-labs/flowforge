package io.flowforge.spring.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringUtils;

import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.registry.MutableWorkflowPlanRegistry;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Discovers @FlowWorkflow @Bean methods that produce WorkflowExecutionPlan
 * and registers them into WorkflowPlanRegistry.
 *
 * Use BeanFactoryPostProcessor for discovery (metadata) and
 * SmartInitializingSingleton
 * for registration to avoid early bean instantiation.
 */
public final class WorkflowPlanRegistrar
        implements BeanFactoryPostProcessor, SmartInitializingSingleton, BeanFactoryAware {

    private ConfigurableListableBeanFactory beanFactory;

    private final MutableWorkflowPlanRegistry workflowPlanRegistry;
    private final Map<String, String> workflowBeansById = new LinkedHashMap<>();

    public WorkflowPlanRegistrar(MutableWorkflowPlanRegistry workflowPlanRegistry) {
        this.workflowPlanRegistry = workflowPlanRegistry;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException("WorkflowPlanRegistrar requires a ConfigurableListableBeanFactory");
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory bf) throws BeansException {
        for (String beanName : bf.getBeanDefinitionNames()) {
            // Use Spring's robust mechanism to find annotation on bean (handles proxies and
            // factory methods)
            FlowWorkflow ann = bf.findAnnotationOnBean(beanName, FlowWorkflow.class);

            if (ann == null) {
                continue;
            }

            String workflowId = ann.id();
            if (!StringUtils.hasText(workflowId)) {
                throw new IllegalStateException("@FlowWorkflow id must not be blank. Bean: " + beanName);
            }

            Class<?> beanType = bf.getType(beanName);
            if (beanType != null && !WorkflowExecutionPlan.class.isAssignableFrom(beanType)) {
                throw new IllegalStateException(
                        "@FlowWorkflow must annotate a bean of type WorkflowExecutionPlan. Bean: " + beanName);
            }

            String previous = workflowBeansById.putIfAbsent(workflowId, beanName);
            if (previous != null) {
                throw new IllegalStateException("Duplicate workflow id: " + workflowId
                        + " (beans: " + previous + ", " + beanName + ")");
            }
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (Map.Entry<String, String> entry : workflowBeansById.entrySet()) {
            String workflowId = entry.getKey();
            String beanName = entry.getValue();

            // Instantiate and register safely now that the context is fully refreshed
            WorkflowExecutionPlan plan = beanFactory.getBean(beanName, WorkflowExecutionPlan.class);

            sanityCheck(plan, workflowId);

            workflowPlanRegistry.register(workflowId, plan);
        }
    }

    private static void sanityCheck(WorkflowExecutionPlan plan, String workflowId) {
        if (plan.nodes().isEmpty()) {
            throw new IllegalStateException("WorkflowExecutionPlan has no nodes. workflowId=" + workflowId);
        }
        if (plan.roots() == null || plan.roots().isEmpty()) {
            throw new IllegalStateException("WorkflowExecutionPlan has no roots. workflowId=" + workflowId);
        }
    }
}