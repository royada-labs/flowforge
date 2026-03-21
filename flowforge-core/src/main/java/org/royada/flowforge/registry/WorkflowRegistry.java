package org.royada.flowforge.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.royada.flowforge.exception.WorkflowConfigurationException;
import org.royada.flowforge.exception.UnknownWorkflowException;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Registry of all known workflow descriptors.
 *
 * <p>It is mutable only during startup registration and becomes immutable once sealed.
 */
public class WorkflowRegistry implements WorkflowPlanRegistry {

    private final Map<String, WorkflowDescriptor> workflows = new ConcurrentHashMap<>();
    private volatile boolean sealed;

    /**
     * Creates a new workflow registry.
     */
    public WorkflowRegistry() {}

    /**
     * Registers a new workflow descriptor.
     * 
     * @param descriptor the workflow descriptor to register
     * @throws WorkflowConfigurationException if the registry is sealed, the descriptor is null, 
     * the ID is blank, or a duplicate ID is found.
     */
    public void register(WorkflowDescriptor descriptor) {
        if (sealed) {
            throw new WorkflowConfigurationException("WorkflowRegistry is immutable after initialization");
        }
        if (descriptor == null) {
            throw new WorkflowConfigurationException("Workflow descriptor must not be null");
        }
        String id = descriptor.id();
        if (id == null || id.isBlank()) {
            throw new WorkflowConfigurationException("Workflow id must not be blank");
        }
        WorkflowExecutionPlan plan = descriptor.plan();
        if (plan == null) {
            throw new WorkflowConfigurationException("Workflow plan must not be null. workflowId=" + id);
        }
        if (plan.nodes().isEmpty()) {
            throw new WorkflowConfigurationException("Workflow plan has no nodes. workflowId=" + id);
        }
        if (plan.roots() == null || plan.roots().isEmpty()) {
            throw new WorkflowConfigurationException("Workflow plan has no roots. workflowId=" + id);
        }

        WorkflowDescriptor previous = workflows.putIfAbsent(id, descriptor);
        if (previous != null) {
            throw new WorkflowConfigurationException(
                    "Duplicate workflow id: " + id
                            + " (sources: " + previous.source().getName() + ", " + descriptor.source().getName() + ")"
            );
        }
    }

    /**
     * Returns the workflow descriptor for the given ID.
     * 
     * @param id the workflow ID
     * @return the workflow descriptor
     * @throws UnknownWorkflowException if the ID is not found
     */
    public WorkflowDescriptor get(String id) {
        WorkflowDescriptor descriptor = workflows.get(id);
        if (descriptor == null) {
            throw new UnknownWorkflowException(id);
        }
        return descriptor;
    }

    /**
     * Returns all registered workflow descriptors.
     * 
     * @return an unmodifiable collection of all descriptors
     */
    public Collection<WorkflowDescriptor> all() {
        return Collections.unmodifiableCollection(workflows.values());
    }

    /**
     * Seals the registry, making it immutable for future registrations.
     */
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
