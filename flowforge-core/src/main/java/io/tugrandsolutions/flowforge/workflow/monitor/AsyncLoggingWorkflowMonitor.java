package io.tugrandsolutions.flowforge.workflow.monitor;

import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.instance.WorkflowInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.*;

public final class AsyncLoggingWorkflowMonitor implements WorkflowMonitor, AutoCloseable {
    private static final Logger log =
            LoggerFactory.getLogger(AsyncLoggingWorkflowMonitor.class);

    private final Clock clock;
    private final ExecutorService executor;

    // start times (millis) with cleanup
    private final ConcurrentMap<Object, Long> workflowStart = new ConcurrentHashMap<>();
    private final ConcurrentMap<TaskKey, Long> taskStart = new ConcurrentHashMap<>();

    public AsyncLoggingWorkflowMonitor() {
        this(Clock.systemUTC(), Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "flowforge-monitor");
            t.setDaemon(true);
            return t;
        }));
    }

    public AsyncLoggingWorkflowMonitor(Clock clock, ExecutorService executor) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void onWorkflowStart(WorkflowInstance instance) {
        long now = clock.millis();
        workflowStart.put(instanceKey(instance), now);

        submit(() -> log.info("Workflow started: instance={} at={}", instanceKey(instance), now));
    }

    @Override
    public void onWorkflowComplete(WorkflowInstance instance) {
        Object key = instanceKey(instance);
        long now = clock.millis();
        Long start = workflowStart.remove(key); // CLEANUP to avoid leaks
        long durationMs = (start == null) ? -1L : (now - start);

        submit(() -> log.info("Workflow completed: instance={} durationMs={}", key, durationMs));

        // Optional extra cleanup: remove any dangling task keys for this instance
        // (only needed if you expect abrupt cancellation without task events)
        taskStart.keySet().removeIf(k -> k.instanceKey.equals(key));
    }

    @Override
    public void onTaskStart(WorkflowInstance instance, TaskId taskId) {
        Object iKey = instanceKey(instance);
        TaskKey tKey = new TaskKey(iKey, taskId);
        long now = clock.millis();
        taskStart.put(tKey, now);

        submit(() -> log.info("Task started: instance={} taskId={} at={}", iKey, taskId, now));
    }

    @Override
    public void onTaskSuccess(WorkflowInstance instance, TaskId taskId) {
        finishTask(instance, taskId, "SUCCESS", null);
    }

    @Override
    public void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {
        finishTask(instance, taskId, "SKIPPED", null);
    }

    @Override
    public void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error) {
        finishTask(instance, taskId, "FAILURE", error);
    }

    private void finishTask(
            WorkflowInstance instance,
            TaskId taskId,
            String outcome,
            Throwable error
    ) {
        Object iKey = instanceKey(instance);
        TaskKey tKey = new TaskKey(iKey, taskId);
        long now = clock.millis();
        Long start = taskStart.remove(tKey); // CLEANUP to avoid leaks
        long durationMs = (start == null) ? -1L : (now - start);

        submit(() -> {
            if (error == null) {
                log.info("Task finished: instance={} taskId={} outcome={} durationMs={}",
                        iKey, taskId, outcome, durationMs);
            } else {
                log.warn("Task finished: instance={} taskId={} outcome={} durationMs={} error={}",
                        iKey, taskId, outcome, durationMs, error.toString(), error);
            }
        });
    }

    private void submit(Runnable runnable) {
        try {
            executor.submit(runnable);
        } catch (RejectedExecutionException ignored) {
            // Monitor is shutting down; ignore to avoid impacting workflow execution
        }
    }

    /**
     * Stable instance identifier for logging + map keys.
     * We avoid relying on WorkflowInstance.equals/hashCode stability.
     */
    private Object instanceKey(WorkflowInstance instance) {
        return System.identityHashCode(instance);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private record TaskKey(Object instanceKey, TaskId taskId) { }
}
