package io.tugrandsolutions.flowforge.workflow.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.tugrandsolutions.flowforge.validation.TypeMetadata;

import io.opentelemetry.api.trace.SpanContext;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

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
    private final Map<String, SpanContext> completedContexts = new ConcurrentHashMap<>();
    private String workflowId;
    private Span workflowSpan;
    private String traceId = "";

    private static final int MAX_ATTRIBUTE_LENGTH = 128;
    private static final int MAX_ATTR_COUNT = 32;

    public OpenTelemetryExecutionTracer(Tracer tracer, Map<String, TypeMetadata> typeInfo) {
        this.tracer = tracer;
        this.typeInfo = Map.copyOf(typeInfo != null ? typeInfo : Collections.emptyMap());
    }

    @Override
    public void onWorkflowStart(String workflowId, String executionId) {
        this.workflowId = workflowId;
        this.workflowSpan = tracer.spanBuilder("flowforge.workflow.execute")
                .setAttribute("flowforge.workflow.id", safeValue(workflowId))
                .setAttribute("flowforge.execution.id", safeValue(executionId))
                .setAttribute("flowforge.system", "flowforge")
                .startSpan();

        this.traceId = workflowSpan.getSpanContext().getTraceId();
    }

    @Override
    public void onWorkflowSuccess() {
        if (workflowSpan != null) {
            if (workflowSpan.isRecording()) {
                workflowSpan.setStatus(StatusCode.OK);
            }
            workflowSpan.end();
            workflowSpan = null;
        }
    }

    @Override
    public void onWorkflowError(Throwable error) {
        if (workflowSpan != null) {
            if (workflowSpan.isRecording()) {
                workflowSpan.recordException(error);
                workflowSpan.setStatus(StatusCode.ERROR);
            }
            workflowSpan.end();
            workflowSpan = null;
        }
    }

    @Override
    public void onWorkflowCanceled() {
        if (workflowSpan != null) {
            if (workflowSpan.isRecording()) {
                workflowSpan.setAttribute("flowforge.status", "CANCELED");
            }
            workflowSpan.end();
            workflowSpan = null;
        }
    }

    @Override
    public void onTaskStart(String taskId, Collection<String> dependencyIds) {
        if (workflowSpan == null) return;

        Context parentContext = Context.current().with(workflowSpan);

        io.opentelemetry.api.trace.SpanBuilder spanBuilder = tracer.spanBuilder("flowforge.task.execute")
                .setParent(parentContext)
                .setAttribute("flowforge.task.id", safeValue(taskId))
                .setAttribute("flowforge.workflow.id", safeValue(workflowId))
                .setAttribute("flowforge.system", "flowforge");

        // Add links to dependencies for complex DAG visualization
        if (dependencyIds != null) {
            for (String depId : dependencyIds) {
                SpanContext depCtx = completedContexts.get(depId);
                if (depCtx != null && depCtx.isValid()) {
                    spanBuilder.addLink(depCtx);
                }
            }
        }

        Span taskSpan = spanBuilder.startSpan();

        if (taskSpan.isRecording()) {
            TypeMetadata types = typeInfo.get(taskId);
            if (types != null) {
                taskSpan.setAttribute("flowforge.task.input.type", safeValue(types.inputType().getSimpleName()));
                taskSpan.setAttribute("flowforge.task.output.type", safeValue(types.outputType().getSimpleName()));
            }
        }

        activeSpans.put(taskId, taskSpan);
    }

    @Override
    @Deprecated
    public void onTaskStart(String taskId) {
        onTaskStart(taskId, Collections.emptyList());
    }

    @Override
    public void onTaskSuccess(String taskId, Object output) {
        Span span = activeSpans.remove(taskId);
        if (span != null) {
            if (span.isRecording()) {
                span.setAttribute("flowforge.task.status", "SUCCESS");
                span.setStatus(StatusCode.OK);
            }
            completedContexts.put(taskId, span.getSpanContext());
            span.end();
        }
    }

    @Override
    public void onTaskSkipped(String taskId) {
        Span span = activeSpans.remove(taskId);
        if (span != null) {
            if (span.isRecording()) {
                span.setAttribute("flowforge.task.status", "SKIPPED");
            }
            completedContexts.put(taskId, span.getSpanContext());
            span.end();
        }
    }

    @Override
    public void onTaskError(String taskId, Throwable error) {
        Span span = activeSpans.remove(taskId);
        if (span != null) {
            if (span.isRecording()) {
                span.recordException(error);
                span.setStatus(StatusCode.ERROR);
                span.setAttribute("flowforge.task.status", "ERROR");
            }
            completedContexts.put(taskId, span.getSpanContext());
            span.end();
        }
    }

    private String safeValue(String value) {
        if (value == null) return "unknown";
        if (value.length() > MAX_ATTRIBUTE_LENGTH) {
            return value.substring(0, MAX_ATTRIBUTE_LENGTH - 3) + "...";
        }
        return value;
    }

    @Override
    public ExecutionTrace build() {
        // This tracer doesn't collect data for a local timeline, 
        // it just signals spans to OTel. 
        // We return an empty trace but with the correct traceId.
        return new ExecutionTrace(Collections.emptyList(), 0, 0, traceId);
    }
}
