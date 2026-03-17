package io.flowforge.workflow.monitor;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.flowforge.task.TaskId;
import io.flowforge.workflow.instance.WorkflowInstance;

public final class AsyncLoggingWorkflowMonitor implements WorkflowMonitor, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AsyncLoggingWorkflowMonitor.class);

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

        submit(() -> {
            String msg = String.format("Workflow started: workflowId=%s instance=%s at=%d",
                    instance.workflowId(), instanceKey(instance), now);
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
            String msg = String.format("Workflow completed: workflowId=%s instance=%s durationMs=%d",
                    instance.workflowId(), key, durationMs);
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
            String msg = String.format("Task started: workflowId=%s instance=%s taskId=%s at=%d",
                    instance.workflowId(), iKey, taskId, now);
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
            String msg;
            if (error == null) {
                msg = String.format("Task finished: workflowId=%s instance=%s taskId=%s outcome=%s durationMs=%d",
                        instance.workflowId(), iKey, taskId, outcome, durationMs);
            } else {
                msg = String.format(
                        "Task finished: workflowId=%s instance=%s taskId=%s outcome=%s durationMs=%d error=%s",
                        instance.workflowId(), iKey, taskId, outcome, durationMs, error.toString());
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
