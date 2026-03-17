package io.tugrandsolutions.flowforge.workflow.trace;

import io.tugrandsolutions.flowforge.validation.TypeMetadata;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionTraceTest {

    @Test
    void should_generate_valid_json_and_pretty_string() {
        TaskExecutionTrace t1 = new TaskExecutionTrace(
                "taskA", ExecutionStatus.SUCCESS, 1000, 1010, 10,
                "thread-1", null, "Void", "Integer"
        );
        TaskExecutionTrace t2 = new TaskExecutionTrace(
                "taskB", ExecutionStatus.ERROR, 1011, 1014, 3,
                "thread-2", "Boom", "Integer", "String"
        );

        ExecutionTrace trace = new ExecutionTrace(java.util.List.of(t1, t2), 1000, 1015);

        String json = trace.toJson();
        assertTrue(json.contains("\"taskId\": \"taskA\""));
        assertTrue(json.contains("\"status\": \"SUCCESS\""));
        assertTrue(json.contains("\"errorMessage\": \"Boom\""));

        String pretty = trace.toPrettyString();
        assertTrue(pretty.contains("taskA"));
        assertTrue(pretty.contains("SUCCESS"));
        assertTrue(pretty.contains("10ms"));
        assertTrue(pretty.contains("(Boom)"));
    }

    @Test
    void tracer_should_capture_events_and_build_trace() {
        Map<String, TypeMetadata> types = Map.of(
                "A", new TypeMetadata(Void.class, Integer.class)
        );
        DefaultExecutionTracer tracer = new DefaultExecutionTracer(types);

        tracer.onTaskStart("A");
        tracer.onTaskSuccess("A", 123);

        ExecutionTrace trace = tracer.build();
        assertEquals(1, trace.tasks().size());
        assertEquals("A", trace.tasks().getFirst().taskId());
        assertEquals(ExecutionStatus.SUCCESS, trace.tasks().getFirst().status());
        assertEquals("Void", trace.tasks().getFirst().inputType());
        assertEquals("Integer", trace.tasks().getFirst().outputType());
    }
}
