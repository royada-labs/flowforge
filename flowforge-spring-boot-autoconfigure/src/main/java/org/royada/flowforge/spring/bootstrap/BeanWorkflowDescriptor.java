package org.royada.flowforge.spring.bootstrap;

import java.util.Objects;

import org.royada.flowforge.registry.WorkflowDescriptor;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

final class BeanWorkflowDescriptor implements WorkflowDescriptor {

    private final String id;
    private final WorkflowExecutionPlan plan;
    private final Class<?> source;

    BeanWorkflowDescriptor(String id, WorkflowExecutionPlan plan, Class<?> source) {
        this.id = requireId(id);
        this.plan = Objects.requireNonNull(plan, "plan");
        this.source = Objects.requireNonNull(source, "source");
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
        return source;
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Workflow id must not be blank");
        }
        return id;
    }
}
