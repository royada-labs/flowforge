package io.flowforge.spring.registry;

import io.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultWorkflowPlanRegistry implements MutableWorkflowPlanRegistry {

    private final Map<String, WorkflowExecutionPlan> plans =
            new ConcurrentHashMap<>();

    public void register(String workflowId, WorkflowExecutionPlan plan) {
        WorkflowExecutionPlan previous = plans.putIfAbsent(workflowId, plan);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate workflow id: " + workflowId
            );
        }
    }

    @Override
    public boolean contains(String workflowId) {
        return plans.containsKey(workflowId);
    }

    @Override
    public Optional<WorkflowExecutionPlan> find(String workflowId) {
        return Optional.ofNullable(plans.get(workflowId));
    }

    @Override
    public Collection<WorkflowExecutionPlan> snapshot() {
        return Collections.unmodifiableCollection(plans.values());
    }
}