package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.spring.annotations.FlowWorkflow;
import io.tugrandsolutions.flowforge.spring.registry.DefaultWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.MutableWorkflowPlanRegistry;
import io.tugrandsolutions.flowforge.spring.registry.WorkflowPlanRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.lang.reflect.Method;

/**
 * Discovers @FlowWorkflow @Bean methods that produce WorkflowExecutionPlan
 * and registers them into WorkflowPlanRegistry.
 *
 * Runs during context bootstrap (before singletons instantiation finishes).
 */
public final class WorkflowPlanRegistrar
        implements BeanFactoryPostProcessor, BeanFactoryAware {

    private BeanFactory beanFactory;

    private final MutableWorkflowPlanRegistry workflowPlanRegistry;

    public WorkflowPlanRegistrar(MutableWorkflowPlanRegistry workflowPlanRegistry) {
        this.workflowPlanRegistry = workflowPlanRegistry;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory bf) throws BeansException {
        // Strategy:
        // 1) iterate bean definitions
        // 2) find factory method (if any)
        // 3) inspect it for @FlowWorkflow
        // 4) if annotated, ensure bean type is WorkflowExecutionPlan
        // 5) eagerly instantiate just those workflow beans and register into registry

        for (String beanName : bf.getBeanDefinitionNames()) {
            BeanDefinition bd = bf.getBeanDefinition(beanName);

            String factoryBeanName = bd.getFactoryBeanName();
            String factoryMethodName = bd.getFactoryMethodName();

            if (!StringUtils.hasText(factoryMethodName) || !StringUtils.hasText(factoryBeanName)) {
                continue;
            }

            // Find the @Bean factory method on the configuration class
            Class<?> factoryType;
            try {
                factoryType = bf.getType(factoryBeanName);
            } catch (Throwable ex) {
                // If type cannot be resolved now, skip.
                continue;
            }
            if (factoryType == null) continue;

            Method factoryMethod = findNoArgOrAnyMatchingMethod(factoryType, factoryMethodName);
            if (factoryMethod == null) continue;

            FlowWorkflow ann = factoryMethod.getAnnotation(FlowWorkflow.class);
            if (ann == null) continue;

            String workflowId = ann.id();
            if (!StringUtils.hasText(workflowId)) {
                throw new IllegalStateException("@FlowWorkflow id must not be blank. Bean: " + beanName);
            }

            // Validate bean return type is WorkflowExecutionPlan (or assignable)
            ResolvableType resolved = ResolvableType.forMethodReturnType(factoryMethod);
            Class<?> raw = resolved.resolve();
            if (raw == null || !WorkflowExecutionPlan.class.isAssignableFrom(raw)) {
                throw new IllegalStateException(
                        "@FlowWorkflow must annotate a @Bean method returning WorkflowExecutionPlan. " +
                                "Bean: " + beanName + ", returnType=" + factoryMethod.getReturnType().getName()
                );
            }

            // Instantiate the workflow bean and register it.
            WorkflowExecutionPlan plan = bf.getBean(beanName, WorkflowExecutionPlan.class);
            if (plan == null) {
                throw new IllegalStateException("WorkflowExecutionPlan bean is null for workflowId=" + workflowId);
            }

            sanityCheck(plan, workflowId);

            // We need register() method; if registry interface doesn't expose it,
            // either cast to default impl or expose register in the interface.
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

    /**
     * Finds a method by name. If overloaded, chooses the first occurrence.
     * In @Configuration classes, @Bean methods are typically unique by name.
     */
    private static Method findNoArgOrAnyMatchingMethod(Class<?> type, String methodName) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }
}