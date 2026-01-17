package io.tugrandsolutions.flowforge.spring.dsl;

public interface FlowBranch {
    FlowBranch then(String taskId);
    FlowBranch fork(java.util.function.Consumer<FlowBranch>... branches);
}