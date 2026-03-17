package io.tugrandsolutions.flowforge.workflow.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.tugrandsolutions.flowforge.validation.TypeMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link ExecutionTracer} that exports events as OpenTelemetry spans.
 *
 * <p>Each workflow execution results in a root span, and each task execution results
 * in a child span. Attributes are added for id, types, and status.
 */
public final class OpenTelemetryExecutionTracer implements ExecutionTracer {

    private final Tracer tracer;
    private final Map<String, TypeMetadata> typeInfo;
    private final Map<String, Span> activeSpans = new ConcurrentHashMap<>();

    private Span workflowSpan;
    private String traceId = "";

    public OpenTelemetryExecutionTracer(Tracer tracer, Map<String, TypeMetadata> typeInfo) {
        this.tracer = tracer;
        this.typeInfo = Map.copyOf(typeInfo != null ? typeInfo : Collections.emptyMap());
    }

    @Override
    public void onWorkflowStart(String workflowId, String executionId) {
        this.workflowSpan = tracer.spanBuilder("flowforge.workflow." + workflowId)
                .setAttribute("flowforge.workflow.id", workflowId)
                .setAttribute("flowforge.execution.id", executionId)
                .startSpan();

        this.traceId = workflowSpan.getSpanContext().getTraceId();
    }

    @Override
    public void onWorkflowSuccess() {
        if (workflowSpan != null) {
            workflowSpan.setStatus(StatusCode.OK);
            workflowSpan.end();
            workflowSpan = null;
        }
    }

    @Override
    public void onWorkflowError(Throwable error) {
        if (workflowSpan != null) {
            workflowSpan.recordException(error);
            workflowSpan.setStatus(StatusCode.ERROR);
            workflowSpan.end();
            workflowSpan = null;
        }
    }

    @Override
    public void onWorkflowCanceled() {
        if (workflowSpan != null) {
            workflowSpan.setAttribute("flowforge.status", "CANCELED");
            workflowSpan.end();
            workflowSpan = null;
        }
    }

    @Override
    public void onTaskStart(String taskId) {
        // We use the root workflow span as parent to ensure correct hierarchy
        // even if Reactor shifts threads between callbacks.
        Context parentContext = workflowSpan != null 
                ? Context.current().with(workflowSpan) 
                : Context.current();

        Span taskSpan = tracer.spanBuilder("flowforge.task." + taskId)
                .setParent(parentContext)
                .setAttribute("flowforge.task.id", taskId)
                .startSpan();

        TypeMetadata types = typeInfo.get(taskId);
        if (types != null) {
            taskSpan.setAttribute("flowforge.task.input.type", types.inputType().getSimpleName());
            taskSpan.setAttribute("flowforge.task.output.type", types.outputType().getSimpleName());
        }

        activeSpans.put(taskId, taskSpan);
    }

    @Override
    public void onTaskSuccess(String taskId, Object output) {
        Span span = activeSpans.remove(taskId);
        if (span != null) {
            span.setAttribute("flowforge.task.status", "SUCCESS");
            span.setStatus(StatusCode.OK);
            span.end();
        }
    }

    @Override
    public void onTaskSkipped(String taskId) {
        Span span = activeSpans.remove(taskId);
        if (span != null) {
            span.setAttribute("flowforge.task.status", "SKIPPED");
            span.end();
        }
    }

    @Override
    public void onTaskError(String taskId, Throwable error) {
        Span span = activeSpans.remove(taskId);
        if (span != null) {
            span.recordException(error);
            span.setStatus(StatusCode.ERROR);
            span.setAttribute("flowforge.task.status", "ERROR");
            span.end();
        }
    }

    @Override
    public ExecutionTrace build() {
        // This tracer doesn't collect data for a local timeline, 
        // it just signals spans to OTel. 
        // We return an empty trace but with the correct traceId.
        return new ExecutionTrace(Collections.emptyList(), 0, 0, traceId);
    }
}
