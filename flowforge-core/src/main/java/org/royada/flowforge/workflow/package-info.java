/**
 * Workflow execution engine and runtime components.
 *
 * <p>This package contains the core runtime for executing workflows:
 *
 * <ul>
 *   <li>{@link org.royada.flowforge.workflow.ReactiveExecutionContext} - Thread-safe context for storing task results</li>
 *   <li>{@link org.royada.flowforge.workflow.instance.WorkflowInstance} - Tracks execution state of a single workflow</li>
 *   <li>{@link org.royada.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator} - Event-driven execution engine</li>
 *   <li>{@link org.royada.flowforge.workflow.graph.TaskNode} - Represents a task in the execution graph</li>
 * </ul>
 *
 * <h2>Execution Model</h2>
 * <p>The orchestrator uses an event-driven model with backpressure control:
 * <ul>
 *   <li>Tasks are emitted to a work sink as they become ready</li>
 *   <li>A flatMap with bounded concurrency controls parallelism</li>
 *   <li>Results are emitted to a state sink for dependency tracking</li>
 * </ul>
 *
 * @see org.royada.flowforge.workflow.ReactiveExecutionContext
 * @see org.royada.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator
 */
package org.royada.flowforge.workflow;
