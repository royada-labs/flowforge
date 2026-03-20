package org.royada.flowforge.workflow.instance;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.trace.ExecutionTracer;

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

    /**
     * Creates a new execution session.
     * 
     * @param instance workflow instance
     * @param tracer execution tracer
     */
    public ExecutionSession(WorkflowInstance instance, ExecutionTracer tracer) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.tracer = Objects.requireNonNull(tracer, "tracer");
    }

    /**
     * Returns the workflow instance.
     * 
     * @return workflow instance
     */
    public WorkflowInstance instance() { return instance; }

    /**
     * Returns the execution tracer.
     * 
     * @return execution tracer
     */
    public ExecutionTracer tracer() { return tracer; }

    /**
     * Marks task start and updates in-flight metrics.
     *
     * @param taskId task id
     */
    public void recordTaskStart(TaskId taskId) {
        taskStartTimes.put(taskId, System.nanoTime());
        int currentInFlight = inFlightCount.incrementAndGet();
        maxInFlightObserved.updateAndGet(max -> Math.max(max, currentInFlight));
    }

    /**
     * Marks task completion and records duration.
     *
     * @param taskId task id
     * @return task duration or {@link Duration#ZERO} when start timestamp was unavailable
     */
    public Duration recordTaskCompletion(TaskId taskId) {
        inFlightCount.decrementAndGet();
        Long startNanos = taskStartTimes.remove(taskId);
        if (startNanos == null) return Duration.ZERO;
        
        Duration duration = Duration.ofNanos(System.nanoTime() - startNanos);
        taskDurations.put(taskId, duration);
        return duration;
    }

    /**
     * Records task error.
     *
     * @param taskId task id
     * @param error failure
     */
    public void recordTaskError(TaskId taskId, Throwable error) {
        taskErrors.put(taskId, error);
    }

    /**
     * Returns the task durations map.
     * 
     * @return task durations map
     */
    public Map<TaskId, Duration> taskDurations() { return taskDurations; }

    /**
     * Returns the task errors map.
     * 
     * @return task errors map
     */
    public Map<TaskId, Throwable> taskErrors() { return taskErrors; }

    /**
     * Returns the maximum concurrent tasks observed.
     * 
     * @return maximum concurrent tasks observed
     */
    public int maxInFlightObserved() { return maxInFlightObserved.get(); }

    /**
     * Returns the current in-flight task count.
     * 
     * @return current in-flight task count
     */
    public int inFlightCount() { return inFlightCount.get(); }
}
