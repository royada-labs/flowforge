package org.royada.flowforge.spring.registry;

import java.util.Collection;
import java.util.Optional;

import org.royada.flowforge.registry.WorkflowDescriptor;
import org.royada.flowforge.registry.WorkflowRegistry;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Default mutable workflow plan registry backed by {@link WorkflowRegistry}.
 */
public class DefaultWorkflowPlanRegistry extends WorkflowRegistry implements MutableWorkflowPlanRegistry {

    /**
     * Creates an empty workflow plan registry.
     */
    public DefaultWorkflowPlanRegistry() {
    }

    @Override
    public void register(String workflowId, WorkflowExecutionPlan plan) {
        register(new LegacyPlanWorkflowDescriptor(workflowId, plan));
    }

    @Override
    public boolean contains(String workflowId) {
        return super.contains(workflowId);
    }

    @Override
    public Optional<WorkflowExecutionPlan> find(String workflowId) {
        return super.find(workflowId);
    }

    @Override
    public Collection<WorkflowExecutionPlan> snapshot() {
        return super.snapshot();
    }

    private static final class LegacyPlanWorkflowDescriptor implements WorkflowDescriptor {

        private final String id;
        private final WorkflowExecutionPlan plan;

        private LegacyPlanWorkflowDescriptor(String id, WorkflowExecutionPlan plan) {
            this.id = id;
            this.plan = plan;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public WorkflowExecutionPlan plan() {
            return plan;
        }

        @Override
        public Class<?> source() {
            return DefaultWorkflowPlanRegistry.class;
        }
    }
}