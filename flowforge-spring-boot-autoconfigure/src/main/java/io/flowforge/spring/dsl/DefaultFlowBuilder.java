package io.flowforge.spring.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import io.flowforge.spring.dsl.internal.FlowGraph;
import io.flowforge.spring.dsl.internal.FlowPlanMaterializer;
import io.flowforge.spring.dsl.internal.TaskReferenceResolver;
import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;
import io.flowforge.validation.DefaultFlowValidator;
import io.flowforge.validation.FlowValidationException;
import io.flowforge.validation.FlowValidationResult;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.policy.ExecutionPolicy;

final class DefaultFlowBuilder implements FlowBuilder {

    private final FlowGraph graph;
    private final FlowPlanMaterializer materializer;
    private final TaskReferenceResolver referenceResolver;

    DefaultFlowBuilder(FlowGraph graph, FlowPlanMaterializer materializer, TaskReferenceResolver referenceResolver) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.referenceResolver = Objects.requireNonNull(referenceResolver, "referenceResolver");
    }


    // -------------------------------------------------------------------------
    // TaskDefinition overrides — capture type metadata into the graph
    // -------------------------------------------------------------------------

    @Override
    public <I, O> TypedFlowBuilder<O> then(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        graph.then(task);
        return new TypedFlowBuilder<>(this, task, referenceResolver);
    }


    @Override
    public <I, O> TypedFlowBuilder<O> join(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        graph.join(task);
        return new TypedFlowBuilder<>(this, task, referenceResolver);
    }

    @Override
    public void applyPolicy(TaskId taskId, ExecutionPolicy policy) {
        graph.applyPolicy(taskId, policy);
    }



    @SafeVarargs
    @Override
    public final FlowBuilder fork(Consumer<FlowBranch>... branches) {
        Objects.requireNonNull(branches, "branches");
        if (branches.length == 0) {
            throw new IllegalArgumentException("fork requires at least 1 branch");
        }

        var branchBuilders = Arrays.stream(branches)
                .map(b -> Objects.requireNonNull(b, "branch"))
                .toList();

        graph.fork(branchBuilders, this, referenceResolver);
        return this;
    }



    // -------------------------------------------------------------------------
    // Build with validation
    // -------------------------------------------------------------------------

    @Override
    public WorkflowExecutionPlan build() {
        WorkflowExecutionPlan plan = materializer.materialize(graph);

        // Run DAG validation with collected type metadata
        FlowValidationResult result = new DefaultFlowValidator()
                .validate(plan, graph.typeMetadata());

        if (!result.isValid()) {
            throw new FlowValidationException(result, plan);
        }

        return plan;
    }
}
