package io.tugrandsolutions.flowforge.workflow.instance;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.trace.ExecutionTracer;

/**
 * Encapsulates the runtime state of a single workflow execution.
 * Inherits the orquestation's metrics and traces to remain stateless.
 */
public final class ExecutionSession {

    private final WorkflowInstance instance;
    private final ExecutionTracer tracer;
    
    private final Map<TaskId, Long> taskStartTimes = new ConcurrentHashMap<>();
    private final Map<TaskId, Duration> taskDurations = new ConcurrentHashMap<>();
    private final Map<TaskId, Throwable> taskErrors = new ConcurrentHashMap<>();
    private final AtomicInteger maxInFlightObserved = new AtomicInteger(0);
    private final AtomicInteger inFlightCount = new AtomicInteger(0);

    public ExecutionSession(WorkflowInstance instance, ExecutionTracer tracer) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.tracer = Objects.requireNonNull(tracer, "tracer");
    }

    public WorkflowInstance instance() { return instance; }
    public ExecutionTracer tracer() { return tracer; }

    public void recordTaskStart(TaskId taskId) {
        taskStartTimes.put(taskId, System.nanoTime());
        int currentInFlight = inFlightCount.incrementAndGet();
        maxInFlightObserved.updateAndGet(max -> Math.max(max, currentInFlight));
    }

    public Duration recordTaskCompletion(TaskId taskId) {
        inFlightCount.decrementAndGet();
        Long startNanos = taskStartTimes.remove(taskId);
        if (startNanos == null) return Duration.ZERO;
        
        Duration duration = Duration.ofNanos(System.nanoTime() - startNanos);
        taskDurations.put(taskId, duration);
        return duration;
    }

    public void recordTaskError(TaskId taskId, Throwable error) {
        taskErrors.put(taskId, error);
    }

    public Map<TaskId, Duration> taskDurations() { return taskDurations; }
    public Map<TaskId, Throwable> taskErrors() { return taskErrors; }
    public int maxInFlightObserved() { return maxInFlightObserved.get(); }
    public int inFlightCount() { return inFlightCount.get(); }
}
