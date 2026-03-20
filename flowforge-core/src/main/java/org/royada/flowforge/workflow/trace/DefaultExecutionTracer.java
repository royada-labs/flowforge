package org.royada.flowforge.workflow.trace;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.validation.TypeMetadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe implementation of {@link ExecutionTracer} using concurrent collections.
 */
public final class DefaultExecutionTracer implements ExecutionTracer {

    private long workflowStartTime;
    private String traceId = "";
    private final Map<TaskId, TypeMetadata> typeInfo;
    
    // Internal mutable state for tasks in flight
    private final Map<TaskId, TaskStartInfo> taskStarts = new ConcurrentHashMap<>();
    private final List<TaskExecutionTrace> completedTraces = new CopyOnWriteArrayList<>();

    public DefaultExecutionTracer(Map<TaskId, TypeMetadata> typeInfo) {
        this.typeInfo = Map.copyOf(typeInfo != null ? typeInfo : Collections.emptyMap());
    }

    @Override
    public void onWorkflowStart(String workflowId, String executionId) {
        this.workflowStartTime = System.currentTimeMillis();
        // Use executionId as traceId if none other is provided by external tools
        this.traceId = executionId;
    }

    @Override
    public void onWorkflowSuccess() {
        // No-op for internal trace
    }

    @Override
    public void onWorkflowError(Throwable error) {
        // No-op for internal trace
    }

    @Override
    public void onWorkflowCanceled() {
        // No-op for internal trace
    }

    @Override
    public void onTaskStart(TaskId taskId, Collection<TaskId> dependencyIds) {
        taskStarts.put(taskId, new TaskStartInfo(
                System.currentTimeMillis(),
                Thread.currentThread().getName()
        ));
    }

    @Override
    public void onTaskSuccess(TaskId taskId, Object output) {
        recordCompletion(taskId, ExecutionStatus.SUCCESS, null);
    }

    @Override
    public void onTaskSkipped(TaskId taskId) {
        recordCompletion(taskId, ExecutionStatus.SKIPPED, null);
    }

    @Override
    public void onTaskError(TaskId taskId, Throwable error) {
        recordCompletion(taskId, ExecutionStatus.ERROR, error != null ? error.getMessage() : "Unknown error");
    }

    private void recordCompletion(TaskId taskId, ExecutionStatus status, String errorMessage) {
        TaskStartInfo startInfo = taskStarts.get(taskId);
        long endTime = System.currentTimeMillis();
        long startTime = startInfo != null ? startInfo.startTime : endTime;
        String threadName = startInfo != null ? startInfo.threadName : Thread.currentThread().getName();

        TypeMetadata types = typeInfo.get(taskId);
        String inputType = types != null ? types.inputType().getSimpleName() : "Unknown";
        String outputType = types != null ? types.outputType().getSimpleName() : "Unknown";

        completedTraces.add(new TaskExecutionTrace(
                taskId.getValue(),
                status,
                startTime,
                endTime,
                endTime - startTime,
                threadName,
                errorMessage,
                inputType,
                outputType
        ));
    }

    @Override
    public ExecutionTrace build() {
        return new ExecutionTrace(
                completedTraces.stream()
                        .sorted(Comparator.comparingLong(TaskExecutionTrace::startTime))
                        .collect(Collectors.toList()),
                workflowStartTime,
                System.currentTimeMillis(),
                traceId
        );
    }

    private record TaskStartInfo(long startTime, String threadName) {}
}
