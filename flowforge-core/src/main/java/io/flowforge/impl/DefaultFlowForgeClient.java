package io.flowforge.impl;

import java.time.Duration;
import java.util.Objects;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.exception.UnknownWorkflowException;
import io.flowforge.exception.WorkflowExecutionException;
import io.flowforge.registry.WorkflowDescriptor;
import io.flowforge.registry.WorkflowRegistry;
import io.flowforge.validation.TypeMetadata;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.trace.ExecutionTrace;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DefaultFlowForgeClient implements FlowForgeClient {

    private final WorkflowRegistry workflowRegistry;
    private final ReactiveWorkflowOrchestrator orchestrator;

    public DefaultFlowForgeClient(
        WorkflowRegistry workflowRegistry,
            ReactiveWorkflowOrchestrator orchestrator) {
    this.workflowRegistry = Objects.requireNonNull(workflowRegistry, "workflowRegistry");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ReactiveExecutionContext> Mono<T> execute(String workflowId, Object input) {
    WorkflowDescriptor descriptor = findDescriptorOrThrow(workflowId);
    WorkflowExecutionPlan plan = descriptor.plan();
        validateInitialInput(workflowId, plan, input);

        return orchestrator.execute(workflowId, plan, input)
                .map(ctx -> (T) ctx)
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<ReactiveExecutionContext> execute(String workflowId, Object input, Duration timeout) {
    WorkflowDescriptor descriptor = findDescriptorOrThrow(workflowId);
    WorkflowExecutionPlan plan = descriptor.plan();
        validateInitialInput(workflowId, plan, input);

        return orchestrator.execute(workflowId, plan, input)
                .timeout(timeout)
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<Object> executeResult(String workflowId, Object input) {
        WorkflowDescriptor descriptor = findDescriptorOrThrow(workflowId);
        WorkflowExecutionPlan plan = descriptor.plan();
        validateInitialInput(workflowId, plan, input);

        return orchestrator.execute(workflowId, plan, input)
                .map(ctx -> io.flowforge.workflow.result.WorkflowResultSelector
                        .select(plan, ctx))
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    @Override
    public Mono<ExecutionTrace> executeWithTrace(String workflowId, Object input) {
        WorkflowDescriptor descriptor = findDescriptorOrThrow(workflowId);
        WorkflowExecutionPlan plan = descriptor.plan();
        validateInitialInput(workflowId, plan, input);

        return orchestrator.executeWithTrace(plan, input, plan.typeMetadata())
                .onErrorMap(e -> new WorkflowExecutionException(workflowId, e));
    }

    private WorkflowDescriptor findDescriptorOrThrow(String workflowId) {
        try {
            return workflowRegistry.get(workflowId);
        } catch (IllegalStateException notFound) {
            throw new UnknownWorkflowException(workflowId);
        }
    }

    private static void validateInitialInput(String workflowId, WorkflowExecutionPlan plan, Object input) {
        Set<Class<?>> rootInputTypes = new LinkedHashSet<>();
        for (TaskNode root : plan.roots()) {
            TypeMetadata metadata = plan.typeMetadata().get(root.id());
            Class<?> inputType = metadata != null ? metadata.inputType() : root.descriptor().task().inputType();
            rootInputTypes.add(inputType);
        }

        boolean requiresInput = rootInputTypes.stream().anyMatch(type -> type != Void.class);
        if (!requiresInput) {
            // Backward-compatible behavior: ignore extra initial input when roots are Void.
            return;
        }

        if (input == null) {
            throw new IllegalArgumentException(
                    "Workflow '" + workflowId + "' requires initial input for root task(s): " + rootInputTypes
            );
        }

        for (Class<?> rootType : rootInputTypes) {
            if (rootType == Void.class) {
                continue;
            }
            if (!rootType.isInstance(input)) {
                throw new IllegalArgumentException(
                        "Workflow '" + workflowId + "' expects initial input compatible with "
                                + rootType.getName() + " but received " + input.getClass().getName()
                );
            }
        }
    }
}
