package io.flowforge.harness;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

@Configuration
public class HarnessWorkflows {

    private static TaskDefinition<Object, Object> task(String id) {
        return TaskDefinition.of(id, Object.class, Object.class);
    }

    @FlowWorkflow(id = "SEQ_S")
    @Bean
    WorkflowExecutionPlan seqSmall(FlowDsl dsl) {
        return dsl.startTyped(task("cpu1"))
                .then(task("nb1"))
                .then(task("cpu2"))
                .then(task("finalize"))
                .build();
    }

    @FlowWorkflow(id = "SEQ_M")
    @Bean
    WorkflowExecutionPlan seqMedium(FlowDsl dsl) {
        return dsl.startTyped(task("cpu1"))
                .then(task("nb6")).then(task("nb7")).then(task("nb8")).then(task("nb9")).then(task("nb10"))
                .then(task("nb11")).then(task("nb12")).then(task("nb13")).then(task("nb14")).then(task("nb15"))
                .then(task("nb16")).then(task("nb17")).then(task("nb18")).then(task("nb19")).then(task("nb20"))
                .then(task("nb21")).then(task("nb22")).then(task("nb23"))
                .then(task("finalize"))
                .build();
    }

    @SuppressWarnings("unchecked")
    @FlowWorkflow(id = "FORK_4_JOIN")
    @Bean
    WorkflowExecutionPlan forkJoin(FlowDsl dsl) {
        return dsl.startTyped(task("cpu1"))
                .fork(
                        br -> br.then(task("nb2")).then(task("cpu2")),
                        br -> br.then(task("nb3")).then(task("cpu3")),
                        br -> br.then(task("nb4")).then(task("cpu4")),
                        br -> br.then(task("nb5")).then(task("cpu5"))
                )
                .join(task("finalize"))
                .build();
    }

    @FlowWorkflow(id = "BLOCKING_MIX")
    @Bean
    WorkflowExecutionPlan blockingMix(FlowDsl dsl) {
        return dsl.startTyped(task("blk1"))
                .then(task("cpu6"))
                .then(task("blk2"))
                .then(task("finalize"))
                .build();
    }
}
