package org.royada.flowforge.workflow.monitor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.workflow.instance.WorkflowInstance;

public final class AsyncLoggingWorkflowMonitor implements WorkflowMonitor, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AsyncLoggingWorkflowMonitor.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

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
        Object key = instanceKey(instance);
        workflowStart.put(key, now);

        submit(() -> {
            String at = formatTimestamp(now);
            String msg = String.format("Workflow started: workflowId=%s instance=%s at=%s",
                    instance.workflowId(), key, at);
            log(msg);
        });
    }

    @Override
    public void onWorkflowComplete(WorkflowInstance instance) {
        Object key = instanceKey(instance);
        long now = clock.millis();
        Long start = workflowStart.remove(key); // CLEANUP to avoid leaks
        long durationMs = (start == null) ? -1L : (now - start);

        submit(() -> {
            String finishedAt = formatTimestamp(now);
            String msg = String.format("Workflow completed: workflowId=%s instance=%s finishedAt=%s durationMs=%d",
                    instance.workflowId(), key, finishedAt, durationMs);
            log(msg);
        });

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

        submit(() -> {
            String at = formatTimestamp(now);
            String msg = String.format("Task started: workflowId=%s instance=%s taskId=%s at=%s",
                    instance.workflowId(), iKey, taskId.getValue(), at);
            log(msg);
        });
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
            Throwable error) {
        Object iKey = instanceKey(instance);
        TaskKey tKey = new TaskKey(iKey, taskId);
        long now = clock.millis();
        Long start = taskStart.remove(tKey); // CLEANUP to avoid leaks
        long durationMs = (start == null) ? -1L : (now - start);

        submit(() -> {
            String finishedAt = formatTimestamp(now);
            String msg;
            if (error == null) {
                msg = String.format(
                    "Task finished: workflowId=%s instance=%s taskId=%s outcome=%s finishedAt=%s durationMs=%d",
                    instance.workflowId(), iKey, taskId.getValue(), outcome, finishedAt, durationMs);
            } else {
                msg = String.format(
                    "Task finished: workflowId=%s instance=%s taskId=%s outcome=%s finishedAt=%s durationMs=%d error=%s",
                    instance.workflowId(), iKey, taskId.getValue(), outcome, finishedAt, durationMs, error.toString());
            }

            if (error != null) {
                if (log.isDebugEnabled()) {
                    log.error(appendThread(msg), error);
                } else {
                    log.warn(msg, error); // Use warn for task failures by default
                }
            } else {
                log(msg);
            }
        });
    }

    private void log(String message) {
        if (log.isDebugEnabled()) {
            log.debug(appendThread(message));
        } else {
            log.info(message);
        }
    }

    private String appendThread(String message) {
        return message + " thread=" + Thread.currentThread().getName();
    }

    private void submit(Runnable runnable) {
        try {
            executor.submit(runnable);
        } catch (RejectedExecutionException ignored) {
            // Monitor is shutting down; ignore to avoid impacting workflow execution
        }
    }

    private String formatTimestamp(long epochMillis) {
        return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC));
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

    private record TaskKey(Object instanceKey, TaskId taskId) {
    }
}
