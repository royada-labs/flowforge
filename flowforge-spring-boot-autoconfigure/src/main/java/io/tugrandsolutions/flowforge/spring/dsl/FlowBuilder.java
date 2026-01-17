package io.tugrandsolutions.flowforge.spring.dsl;

public interface FlowBuilder {
    FlowBuilder then(String taskId);

    FlowBuilder fork(java.util.function.Consumer<FlowBranch>... branches);

    FlowBuilder join(String taskId);

    io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan build();
}