package io.tugrandsolutions.flowforge.spring.dsl;

import io.tugrandsolutions.flowforge.dsl.TypedTaskNode;
import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowGraph;
import io.tugrandsolutions.flowforge.spring.dsl.internal.FlowPlanMaterializer;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.validation.DefaultFlowValidator;
import io.tugrandsolutions.flowforge.validation.FlowValidationException;
import io.tugrandsolutions.flowforge.validation.FlowValidationResult;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

final class DefaultFlowBuilder implements FlowBuilder {

    private final FlowGraph graph;
    private final FlowPlanMaterializer materializer;

    DefaultFlowBuilder(FlowGraph graph, FlowPlanMaterializer materializer) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    @Override
    public FlowBuilder then(String taskId) {
        graph.then(taskId);
        return this;
    }

    // -------------------------------------------------------------------------
    // TaskDefinition overrides — capture type metadata into the graph
    // -------------------------------------------------------------------------

    @Override
    public <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        graph.then(task.idValue());
        graph.registerTypeMetadata(task.idValue(), task.inputType(), task.outputType());
        return new TypedTaskNode<>(task.toRef());
    }

    @Override
    public <I, O> TypedTaskNode<O> then(TaskDefinition<I, O> task, TypedTaskNode<I> input) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(input, "input");

        // Runtime type validation (fail-fast for raw-type misuse)
        Class<?> expectedInput = task.inputType();
        Class<?> providedOutput = input.ref().outputType();
        if (!expectedInput.isAssignableFrom(providedOutput)) {
            throw new IllegalArgumentException(
                    "Type mismatch in workflow definition: task '" + task.idValue()
                            + "' expects input type " + expectedInput.getName()
                            + " but received output type " + providedOutput.getName()
                            + " from task '" + input.ref().idValue() + "'"
            );
        }

        graph.then(task.idValue());
        graph.registerTypeMetadata(task.idValue(), task.inputType(), task.outputType());
        return new TypedTaskNode<>(task.toRef());
    }

    @Override
    public <I, O> TypedTaskNode<O> join(TaskDefinition<I, O> task) {
        Objects.requireNonNull(task, "task");
        graph.join(task.idValue());
        graph.registerTypeMetadata(task.idValue(), task.inputType(), task.outputType());
        return new TypedTaskNode<>(task.toRef());
    }

    // -------------------------------------------------------------------------
    // Fork / Join (string-based, unchanged)
    // -------------------------------------------------------------------------

    @SafeVarargs
    @Override
    public final FlowBuilder fork(Consumer<FlowBranch>... branches) {
        Objects.requireNonNull(branches, "branches");
        if (branches.length == 0) {
            throw new IllegalArgumentException("fork requires at least 1 branch");
        }

        var branchBuilders = Arrays.stream(branches)
                .map(b -> (Consumer<FlowBranch>) Objects.requireNonNull(b, "branch"))
                .toList();

        graph.fork(branchBuilders);
        return this;
    }

    @Override
    public FlowBuilder join(String taskId) {
        graph.join(taskId);
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