/**
 * Exception hierarchy for FlowForge workflow engine.
 *
 * <p>All exceptions in this package extend {@link io.flowforge.exception.FlowForgeException},
 * which is a runtime exception. This allows workflows to fail fast without explicit
 * exception handling while maintaining type safety.
 *
 * <h2>Exception Categories</h2>
 * <ul>
 *   <li><b>Configuration:</b> {@link WorkflowConfigurationException}, {@link TaskRegistrationException}</li>
 *   <li><b>Execution:</b> {@link ExecutionException}, {@link WorkflowExecutionException}</li>
 *   <li><b>Runtime:</b> {@link TypeMismatchException}, {@link DeadEndException}</li>
 *   <li><b>Lookup:</b> {@link UnknownWorkflowException}</li>
 * </ul>
 *
 * @see io.flowforge.exception.FlowForgeException
 */
package io.flowforge.exception;
