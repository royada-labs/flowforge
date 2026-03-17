package io.flowforge.spring.bootstrap;

import io.flowforge.api.FlowForgeClient;
import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.spring.annotations.FlowWorkflow;
import io.flowforge.spring.autoconfig.FlowForgeAutoConfiguration;
import io.flowforge.spring.dsl.FlowDsl;
import io.flowforge.task.TaskDefinition;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;
import io.flowforge.workflow.trace.ExecutionStatus;
import io.flowforge.workflow.trace.ExecutionTrace;
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
        assertNotNull(trace.traceId());
        assertFalse(trace.traceId().isEmpty());
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

            return dsl.startTyped(s1)
                    .then(s2)
                    .then(s3)
                    .build();
        }

        @Bean @FlowTask(id = "Step1") io.flowforge.api.FlowTaskHandler<Void, Integer> s1() {
            return (i, c) -> Mono.just(42);
        }

        @Bean @FlowTask(id = "Step2") io.flowforge.api.FlowTaskHandler<Integer, String> s2() {
            return (i, c) -> Mono.just("Value: " + i);
        }

        @Bean @FlowTask(id = "Step3") io.flowforge.api.FlowTaskHandler<String, Integer> s3() {
            return (i, c) -> Mono.just(0);
        }
    }
}
