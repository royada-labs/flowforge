/**
 * Annotations for declarative workflow and task definition.
 *
 * <p>These annotations provide a declarative way to define workflows and tasks
 * using Spring's component scanning.
 *
 * <h2>Core Annotations</h2>
 * <ul>
 *   <li>{@link io.flowforge.spring.annotations.TaskHandler} - Marks a class containing task methods</li>
 *   <li>{@link io.flowforge.spring.annotations.FlowTask} - Marks a method as a task implementation</li>
 *   <li>{@link io.flowforge.spring.annotations.FlowWorkflow} - Marks a bean as a workflow definition</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @TaskHandler("customer")
 * public class CustomerTasks {
 *     
 *     @FlowTask(id = "getUser")
 *     public Mono<User> getUser(Void input, ReactiveExecutionContext ctx) {
 *         return userRepository.findById(1L);
 *     }
 * }
 * 
 * @Configuration
 * public class Workflows {
 *     @FlowWorkflow(id = "onboarding")
 *     @Bean
 *     public WorkflowExecutionPlan onboardingFlow(FlowDsl dsl) {
 *         return dsl.flow(CustomerTasks::getUser)
 *                   .then(NotificationTasks::sendWelcome)
 *                   .build();
 *     }
 * }
 * }</pre>
 *
 * @see io.flowforge.spring.annotations.TaskHandler
 * @see io.flowforge.spring.annotations.FlowTask
 * @see io.flowforge.spring.annotations.FlowWorkflow
 */
package io.flowforge.spring.annotations;
