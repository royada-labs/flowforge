package io.flowforge.harness;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

@Configuration
public class HarnessWorkflows {

    @FlowWorkflow(id = "SEQ_S")
    @Bean
    WorkflowExecutionPlan seqSmall(FlowDsl dsl) {
        return dsl.start("cpu1")
                .then("nb1")
                .then("cpu2")
                .then("finalize")
                .build();
    }

    @FlowWorkflow(id = "SEQ_M")
    @Bean
    WorkflowExecutionPlan seqMedium(FlowDsl dsl) {
        // 20 pasos aprox sin repetir IDs
        return dsl.start("cpu1")
                .then("nb6").then("nb7").then("nb8").then("nb9").then("nb10")
                .then("nb11").then("nb12").then("nb13").then("nb14").then("nb15")
                .then("nb16").then("nb17").then("nb18").then("nb19").then("nb20")
                .then("nb21").then("nb22").then("nb23")
                .then("finalize")
                .build();
    }

    @SuppressWarnings("unchecked")
    @FlowWorkflow(id = "FORK_4_JOIN")
    @Bean
    WorkflowExecutionPlan forkJoin(FlowDsl dsl) {
        return dsl.start("cpu1")
                .fork(
                        br -> br.then("nb2").then("cpu2"),
                        br -> br.then("nb3").then("cpu3"),
                        br -> br.then("nb4").then("cpu4"),
                        br -> br.then("nb5").then("cpu5")
                )
                .join("finalize")
                .build();
    }

    @FlowWorkflow(id = "BLOCKING_MIX")
    @Bean
    WorkflowExecutionPlan blockingMix(FlowDsl dsl) {
        return dsl.start("blk1")
                .then("cpu6")
                .then("blk2")
                .then("finalize")
                .build();
    }
}
