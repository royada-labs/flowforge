package org.royada.flowforge.harness;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.royada.flowforge.spring.annotations.FlowWorkflow;
import org.royada.flowforge.spring.dsl.FlowDsl;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

@Configuration
public class HarnessWorkflows {

    @FlowWorkflow(id = "SEQ_S")
    @Bean
    WorkflowExecutionPlan seqSmall(FlowDsl dsl) {
        return dsl.flow(HarnessTaskHandlers::cpu1)
                .then(HarnessTaskHandlers::nb1)
                .then(HarnessTaskHandlers::cpu2)
                .then(HarnessTaskHandlers::finalizeTask)
                .build();
    }

    @FlowWorkflow(id = "SEQ_M")
    @Bean
    WorkflowExecutionPlan seqMedium(FlowDsl dsl) {
        return dsl.flow(HarnessTaskHandlers::cpu1)
                .then(HarnessTaskHandlers::nb6).then(HarnessTaskHandlers::nb7).then(HarnessTaskHandlers::nb8)
                .then(HarnessTaskHandlers::nb9).then(HarnessTaskHandlers::nb10).then(HarnessTaskHandlers::nb11)
                .then(HarnessTaskHandlers::nb12).then(HarnessTaskHandlers::nb13).then(HarnessTaskHandlers::nb14)
                .then(HarnessTaskHandlers::nb15).then(HarnessTaskHandlers::nb16).then(HarnessTaskHandlers::nb17)
                .then(HarnessTaskHandlers::nb18).then(HarnessTaskHandlers::nb19).then(HarnessTaskHandlers::nb20)
                .then(HarnessTaskHandlers::nb21).then(HarnessTaskHandlers::nb22).then(HarnessTaskHandlers::nb23)
                .then(HarnessTaskHandlers::finalizeTask)
                .build();
    }

    @FlowWorkflow(id = "FORK_4_JOIN")
    @Bean
    WorkflowExecutionPlan forkJoin(FlowDsl dsl) {
        return dsl.flow(HarnessTaskHandlers::cpu1)
                .fork(
                        br -> br.then(HarnessTaskHandlers::nb2).then(HarnessTaskHandlers::cpu2),
                        br -> br.then(HarnessTaskHandlers::nb3).then(HarnessTaskHandlers::cpu3),
                        br -> br.then(HarnessTaskHandlers::nb4).then(HarnessTaskHandlers::cpu4),
                        br -> br.then(HarnessTaskHandlers::nb5).then(HarnessTaskHandlers::cpu5)
                )
                .join(HarnessTaskHandlers::finalizeTask)
                .build();
    }

    @FlowWorkflow(id = "BLOCKING_MIX")
    @Bean
    WorkflowExecutionPlan blockingMix(FlowDsl dsl) {
        return dsl.flow(HarnessTaskHandlers::blk1)
                .then(HarnessTaskHandlers::cpu6)
                .then(HarnessTaskHandlers::blk2)
                .then(HarnessTaskHandlers::finalizeTask)
                .build();
    }

    @FlowWorkflow(id = "MID_REALISTIC")
    @Bean
    WorkflowExecutionPlan midRealistic(FlowDsl dsl) {
        return dsl.flow(HarnessTaskHandlers::validateInput)
                .fork(
                        br -> br.then(HarnessTaskHandlers::profileService),
                        br -> br.then(HarnessTaskHandlers::pricingService),
                        br -> br.then(HarnessTaskHandlers::riskService)
                )
                .join(HarnessTaskHandlers::aggregateOffer)
                .then(HarnessTaskHandlers::persistAudit)
                .build();
    }
}
