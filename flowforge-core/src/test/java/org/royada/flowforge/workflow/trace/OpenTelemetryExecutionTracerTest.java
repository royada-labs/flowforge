package org.royada.flowforge.workflow.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.validation.TypeMetadata;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;

class OpenTelemetryExecutionTracerTest {

    @RegisterExtension
    static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    @Test
    void shouldProduceWorkflowAndTaskSpans() {
        // Given
        OpenTelemetryExecutionTracer tracer = new OpenTelemetryExecutionTracer(
                otelTesting.getOpenTelemetry().getTracer(getClass().getName()),
                Map.of(TaskId.of("task1"), new TypeMetadata(String.class, Integer.class))
        );

        // When
        tracer.onWorkflowStart("my-workflow", "exec-123");
        tracer.onTaskStart(TaskId.of("task1"), java.util.List.of());
        tracer.onTaskSuccess(TaskId.of("task1"), 42);
        tracer.onWorkflowSuccess();

        // Then
        List<SpanData> spans = otelTesting.getSpans();
        assertThat(spans).hasSize(2);

        SpanData workflowSpan = spans.stream()
                .filter(s -> s.getName().equals("flowforge.workflow.execute"))
                .findFirst().orElseThrow();
        
        SpanData taskSpan = spans.stream()
                .filter(s -> s.getName().equals("flowforge.task.task1"))
                .findFirst().orElseThrow();

        assertThat(workflowSpan.getAttributes().asMap())
                .containsEntry(io.opentelemetry.api.common.AttributeKey.stringKey("flowforge.workflow.id"), "my-workflow")
                .containsEntry(io.opentelemetry.api.common.AttributeKey.stringKey("flowforge.execution.id"), "exec-123");

        assertThat(taskSpan.getParentSpanId()).isEqualTo(workflowSpan.getSpanId());
        assertThat(taskSpan.getAttributes().asMap())
                .containsEntry(io.opentelemetry.api.common.AttributeKey.stringKey("flowforge.task.id"), "task1")
                .containsEntry(io.opentelemetry.api.common.AttributeKey.stringKey("flowforge.task.input.type"), "String")
                .containsEntry(io.opentelemetry.api.common.AttributeKey.stringKey("flowforge.task.output.type"), "Integer")
                .containsEntry(io.opentelemetry.api.common.AttributeKey.stringKey("flowforge.task.status"), "SUCCESS");
    }

    @Test
    void shouldRecordErrorsOnSpans() {
        // Given
        OpenTelemetryExecutionTracer tracer = new OpenTelemetryExecutionTracer(
                otelTesting.getOpenTelemetry().getTracer(getClass().getName()),
                Map.of()
        );
        RuntimeException error = new RuntimeException("Boom");

        // When
        tracer.onWorkflowStart("err-flow", "exec-error");
        tracer.onTaskStart(TaskId.of("task-fail"), java.util.List.of());
        tracer.onTaskError(TaskId.of("task-fail"), error);
        tracer.onWorkflowError(error);

        // Then
        List<SpanData> spans = otelTesting.getSpans();
        
        SpanData workflowSpan = spans.stream()
                .filter(s -> s.getName().equals("flowforge.workflow.execute"))
                .findFirst().orElseThrow();
        
        SpanData taskSpan = spans.stream()
                .filter(s -> s.getName().equals("flowforge.task.task-fail"))
                .findFirst().orElseThrow();

        assertThat(workflowSpan.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(taskSpan.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(taskSpan.getAttributes().asMap())
                .containsEntry(io.opentelemetry.api.common.AttributeKey.stringKey("flowforge.task.status"), "ERROR");
    }
}
