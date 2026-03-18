package io.flowforge.registry;

import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all known workflow descriptors.
 *
 * <p>It is mutable only during startup registration and becomes immutable once sealed.
 */
public class WorkflowRegistry implements WorkflowPlanRegistry {

    private final Map<String, WorkflowDescriptor> workflows = new ConcurrentHashMap<>();
    private volatile boolean sealed;

    public void register(WorkflowDescriptor descriptor) {
        if (sealed) {
            throw new IllegalStateException("WorkflowRegistry is immutable after initialization");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("Workflow descriptor must not be null");
        }
        String id = descriptor.id();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Workflow id must not be blank");
        }
        WorkflowExecutionPlan plan = descriptor.plan();
        if (plan == null) {
            throw new IllegalArgumentException("Workflow plan must not be null. workflowId=" + id);
        }
        if (plan.nodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow plan has no nodes. workflowId=" + id);
        }
        if (plan.roots() == null || plan.roots().isEmpty()) {
            throw new IllegalArgumentException("Workflow plan has no roots. workflowId=" + id);
        }

        WorkflowDescriptor previous = workflows.putIfAbsent(id, descriptor);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate workflow id: " + id
                            + " (sources: " + previous.source().getName() + ", " + descriptor.source().getName() + ")"
            );
        }
    }

    public WorkflowDescriptor get(String id) {
        WorkflowDescriptor descriptor = workflows.get(id);
        if (descriptor == null) {
            throw new IllegalStateException("Workflow not found: " + id);
        }
        return descriptor;
    }

    public Collection<WorkflowDescriptor> all() {
        return Collections.unmodifiableCollection(workflows.values());
    }

    public void seal() {
        this.sealed = true;
    }

    @Override
    public boolean contains(String workflowId) {
        return workflows.containsKey(workflowId);
    }

    @Override
    public Optional<WorkflowExecutionPlan> find(String workflowId) {
        WorkflowDescriptor descriptor = workflows.get(workflowId);
        return Optional.ofNullable(descriptor != null ? descriptor.plan() : null);
    }

    @Override
    public Collection<WorkflowExecutionPlan> snapshot() {
        Map<String, WorkflowExecutionPlan> plans = new LinkedHashMap<>();
        for (Map.Entry<String, WorkflowDescriptor> entry : workflows.entrySet()) {
            plans.put(entry.getKey(), entry.getValue().plan());
        }
        return Collections.unmodifiableCollection(plans.values());
    }
}
