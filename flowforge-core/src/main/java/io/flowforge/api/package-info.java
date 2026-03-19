/**
 * Public API for FlowForge workflow engine.
 *
 * <p>This package contains the public interfaces and classes that users
 * of the library interact with.
 *
 * <ul>
 *   <li>{@link io.flowforge.api.FlowForgeClient} - Main client for executing workflows</li>
 *   <li>{@link io.flowforge.api.FlowTaskHandler} - Interface for task implementations</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Autowired
 * FlowForgeClient client;
 *
 * Mono<ReactiveExecutionContext> result = client.execute("workflow-id", input);
 * }</pre>
 *
 * @see io.flowforge.api.FlowForgeClient
 */
package io.flowforge.api;
