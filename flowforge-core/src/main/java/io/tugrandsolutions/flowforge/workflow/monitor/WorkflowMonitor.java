package io.tugrandsolutions.flowforge.workflow.monitor;

import io.tugrandsolutions.flowforge.task.TaskId;
import io.tugrandsolutions.flowforge.workflow.instance.WorkflowInstance;

public interface WorkflowMonitor {

    default void onWorkflowStart(WorkflowInstance instance) {}

    default void onWorkflowComplete(WorkflowInstance instance) {}

    default void onTaskStart(WorkflowInstance instance, TaskId taskId) {}

    default void onTaskSuccess(WorkflowInstance instance, TaskId taskId) {}

    default void onTaskSkipped(WorkflowInstance instance, TaskId taskId) {}

    default void onTaskFailure(WorkflowInstance instance, TaskId taskId, Throwable error) {}
}