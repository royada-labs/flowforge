package io.flowforge.spring.registry;

import io.flowforge.registry.WorkflowDescriptor;
import io.flowforge.registry.WorkflowRegistry;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Collection;
import java.util.Optional;

public class DefaultWorkflowPlanRegistry extends WorkflowRegistry implements MutableWorkflowPlanRegistry {

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