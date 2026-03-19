/**
 * Task definition and execution contracts.
 *
 * <p>This package contains the core abstractions for defining workflow tasks:
 *
 * <ul>
 *   <li>{@link io.flowforge.task.Task} - The main interface for task implementations</li>
 *   <li>{@link io.flowforge.task.TaskId} - Unique identifier for tasks</li>
 *   <li>{@link io.flowforge.task.TaskDefinition} - Metadata about a task (input/output types)</li>
 *   <li>{@link io.flowforge.task.BasicTask} - Abstract base class for simple tasks</li>
 * </ul>
 *
 * <h2>Creating a Task</h2>
 * <pre>{@code
 * public class MyTask extends BasicTask<Input, Output> {
 *     protected Mono<Output> doExecute(Input input, ReactiveExecutionContext ctx) {
 *         // Task logic here
 *         return Mono.just(result);
 *     }
 * }
 * }</pre>
 *
 * @see io.flowforge.task.Task
 * @see io.flowforge.task.BasicTask
 */
package io.flowforge.task;
