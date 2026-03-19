package io.flowforge.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.flowforge.task.TaskId;

class ExceptionHierarchyTest {

    @Test
    void deadEndException_should_contain_pending_tasks() {
        TaskId taskA = TaskId.of("taskA");
        TaskId taskB = TaskId.of("taskB");
        Set<TaskId> pending = Set.of(taskA, taskB);
        
        DeadEndException exception = new DeadEndException("Dead-end detected", pending);
        
        assertEquals("Dead-end detected", exception.getMessage());
        assertEquals(pending, exception.getPendingTasks());
        assertEquals(2, exception.getPendingTasks().size());
        assertTrue(exception.getPendingTasks().contains(taskA));
        assertTrue(exception.getPendingTasks().contains(taskB));
    }

    @Test
    void unknownWorkflowException_should_have_clear_message() {
        String workflowId = "my-workflow";
        
        UnknownWorkflowException exception = new UnknownWorkflowException(workflowId);
        
        assertTrue(exception.getMessage().contains(workflowId));
        assertTrue(exception.getMessage().contains("Unknown workflow"));
    }

    @Test
    void unknownWorkflowException_should_extend_flowForgeException() {
        UnknownWorkflowException exception = new UnknownWorkflowException("test");
        
        assertInstanceOf(FlowForgeException.class, exception);
    }

    @Test
    void workflowConfigurationException_should_preserve_message() {
        String message = "Invalid workflow configuration";
        
        WorkflowConfigurationException exception = new WorkflowConfigurationException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void workflowConfigurationException_should_wrap_cause() {
        String message = "Config error";
        Throwable cause = new IllegalStateException("Original error");
        
        WorkflowConfigurationException exception = new WorkflowConfigurationException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void taskRegistrationException_should_preserve_message() {
        String message = "Task registration failed";
        
        TaskRegistrationException exception = new TaskRegistrationException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void taskRegistrationException_should_wrap_cause() {
        String message = "Registration error";
        Throwable cause = new RuntimeException("Original");
        
        TaskRegistrationException exception = new TaskRegistrationException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void workflowExecutionException_should_wrap_nested_exceptions() {
        String workflowId = "test-flow";
        Throwable nestedCause = new IllegalArgumentException("Invalid argument");
        
        WorkflowExecutionException exception = new WorkflowExecutionException(workflowId, nestedCause);
        
        assertTrue(exception.getMessage().contains(workflowId));
        assertEquals(nestedCause, exception.getCause());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    void typeMismatchException_should_have_descriptive_message() {
        TaskId taskId = TaskId.of("mismatch-task");
        Class<String> expected = String.class;
        Class<Integer> actual = Integer.class;
        
        TypeMismatchException exception = new TypeMismatchException(taskId, expected, actual);
        
        assertTrue(exception.getMessage().contains("String"));
        assertTrue(exception.getMessage().contains("Integer"));
        assertTrue(exception.getMessage().contains(taskId.getValue()));
        assertEquals(taskId, exception.getTaskId());
        assertEquals(expected, exception.getExpectedType());
        assertEquals(actual, exception.getActualType());
    }

    @Test
    void executionException_should_wrap_message() {
        String message = "Task execution failed";
        
        ExecutionException exception = new ExecutionException(message);
        
        assertEquals(message, exception.getMessage());
    }

    @Test
    void executionException_should_wrap_cause() {
        String message = "error";
        Throwable cause = new RuntimeException("Original");
        
        ExecutionException exception = new ExecutionException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
