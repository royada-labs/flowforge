package org.royada.flowforge.spring.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.royada.flowforge.task.BasicTask;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;
import org.royada.flowforge.workflow.plan.WorkflowPlanBuilder;
import reactor.core.publisher.Mono;

class ClassWorkflowDescriptorTest {

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

        ClassWorkflowDescriptor descriptor = new ClassWorkflowDescriptor(
                "class-flow",
                plan,
                ClassWorkflowDescriptorTest.class
        );

        assertEquals("class-flow", descriptor.id());
        assertSame(plan, descriptor.plan());
        assertEquals(ClassWorkflowDescriptorTest.class, descriptor.source());
    }
}
