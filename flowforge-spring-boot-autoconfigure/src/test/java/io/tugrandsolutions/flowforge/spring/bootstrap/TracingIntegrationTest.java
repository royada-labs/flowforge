package io.tugrandsolutions.flowforge.spring.bootstrap;

import io.tugrandsolutions.flowforge.api.FlowForgeClient;
import io.tugrandsolutions.flowforge.spring.annotations.FlowTask;
import io.tugrandsolutions.flowforge.spring.annotations.FlowWorkflow;
import io.tugrandsolutions.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.tugrandsolutions.flowforge.spring.dsl.FlowDsl;
import io.tugrandsolutions.flowforge.task.TaskDefinition;
import io.tugrandsolutions.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.tugrandsolutions.flowforge.workflow.trace.ExecutionStatus;
import io.tugrandsolutions.flowforge.workflow.trace.ExecutionTrace;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TracingIntegrationTest.TestConfig.class)
class TracingIntegrationTest {

    @Test
    void executeWithTrace_should_provide_detailed_metrics(
            @org.springframework.beans.factory.annotation.Autowired FlowForgeClient client
    ) {
        ExecutionTrace trace = client.executeWithTrace("tracing-workflow", null)
                .block(java.time.Duration.ofSeconds(5));

        assertNotNull(trace);
        assertEquals(3, trace.tasks().size());

        var t1 = trace.tasks().stream().filter(t -> t.taskId().equals("Step1")).findFirst().orElseThrow();
        assertEquals(ExecutionStatus.SUCCESS, t1.status());
        assertEquals("Void", t1.inputType());
        assertEquals("Integer", t1.outputType());

        var t2 = trace.tasks().stream().filter(t -> t.taskId().equals("Step2")).findFirst().orElseThrow();
        assertEquals(ExecutionStatus.SUCCESS, t2.status());

        var t3 = trace.tasks().stream().filter(t -> t.taskId().equals("Step3")).findFirst().orElseThrow();
        assertEquals(ExecutionStatus.SUCCESS, t3.status());
        assertEquals("String", t3.inputType());
        assertEquals("Integer", t3.outputType());

        System.out.println(trace.toPrettyString());
    }

    @Configuration
    @Import(FlowForgeAutoConfiguration.class)
    static class TestConfig {

        @Bean
        @FlowWorkflow(id = "tracing-workflow")
        WorkflowExecutionPlan workflow(FlowDsl dsl) {
            TaskDefinition<Void, Integer> s1 = TaskDefinition.of("Step1", Void.class, Integer.class);
            TaskDefinition<Integer, String> s2 = TaskDefinition.of("Step2", Integer.class, String.class);
            TaskDefinition<String, Integer> s3 = TaskDefinition.of("Step3", String.class, Integer.class);

            var start = dsl.startTyped(s1);
            var step2Node = start.builder().then(s2, start.node());
            start.builder().then(s3, step2Node);

            return start.builder().build();
        }

        @Bean @FlowTask(id = "Step1") io.tugrandsolutions.flowforge.api.FlowTaskHandler<Void, Integer> s1() {
            return (i, c) -> Mono.just(42);
        }

        @Bean @FlowTask(id = "Step2") io.tugrandsolutions.flowforge.api.FlowTaskHandler<Integer, String> s2() {
            return (i, c) -> Mono.just("Value: " + i);
        }

        @Bean @FlowTask(id = "Step3") io.tugrandsolutions.flowforge.api.FlowTaskHandler<String, Integer> s3() {
            return (i, c) -> Mono.just(0);
        }
    }
}
