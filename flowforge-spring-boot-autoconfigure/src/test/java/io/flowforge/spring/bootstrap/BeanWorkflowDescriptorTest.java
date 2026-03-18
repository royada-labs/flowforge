package io.flowforge.spring.bootstrap;

import io.flowforge.task.BasicTask;
import io.flowforge.task.TaskId;
import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.plan.WorkflowPlanBuilder;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BeanWorkflowDescriptorTest {

    @Test
    void should_expose_id_plan_and_source() {
        WorkflowExecutionPlan plan = WorkflowPlanBuilder.build(List.of(
                new BasicTask<Void, Integer>(TaskId.of("A"), Void.class, Integer.class) {
                    @Override
                    protected Mono<Integer> doExecute(Void input, ReactiveExecutionContext ctx) {
                        return Mono.just(1);
                    }
                }
        ));

        BeanWorkflowDescriptor descriptor = new BeanWorkflowDescriptor(
                "bean-flow",
                plan,
                BeanWorkflowDescriptorTest.class
        );

        assertEquals("bean-flow", descriptor.id());
        assertSame(plan, descriptor.plan());
        assertEquals(BeanWorkflowDescriptorTest.class, descriptor.source());
    }
}
