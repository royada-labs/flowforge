package io.flowforge.spring.bootstrap;

import io.flowforge.registry.WorkflowDescriptor;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Objects;

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
