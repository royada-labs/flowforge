/**
 * Workflow and task registration.
 *
 * <p>This package manages the registry of available workflows and their plans.
 *
 * <ul>
 *   <li>{@link org.royada.flowforge.registry.WorkflowRegistry} - Central registry for workflow definitions</li>
 *   <li>{@link org.royada.flowforge.registry.WorkflowDescriptor} - Metadata about a registered workflow</li>
 *   <li>{@link org.royada.flowforge.registry.WorkflowPlanRegistry} - Interface for workflow plan lookup</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Workflows are registered during application startup</li>
 *   <li>The registry is sealed after all registrations complete</li>
 *   <li>Runtime lookups are O(1) via ConcurrentHashMap</li>
 * </ol>
 *
 * @see org.royada.flowforge.registry.WorkflowRegistry
 */
package org.royada.flowforge.registry;
