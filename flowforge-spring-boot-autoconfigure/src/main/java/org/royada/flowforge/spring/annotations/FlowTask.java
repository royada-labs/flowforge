package org.royada.flowforge.spring.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a task bean or task handler method that can be used in FlowForge workflows.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowTask {
    /**
     * Unique task identifier within the application context.
     *
     * @return task id
     */
    String id();

    /**
     * Marks the task as optional. Optional failures are treated as skipped.
     *
     * @return whether the task is optional
     */
    boolean optional() default false;

    /**
     * Declares upstream task ids required by this task.
     *
     * @return dependency task ids
     */
    String[] dependsOn() default {};

    /**
     * Explicit task input type. Use {@link Object} to rely on inferred type.
     *
     * @return input type class
     */
    Class<?> inputType() default Object.class;

    /**
     * Explicit task output type. Use {@link Object} to rely on inferred type.
     *
     * @return output type class
     */
    Class<?> outputType() default Object.class;

    /**
     * Maximum retry attempts for this task. Negative value disables retry policy via annotation.
     *
     * @return max retries
     */
    int retryMaxRetries() default -1;

    /**
     * Fixed backoff delay in milliseconds between retries. Negative value disables backoff.
     *
     * @return backoff in milliseconds
     */
    long retryBackoffMillis() default -1;

    /**
     * Per-task timeout in milliseconds. Negative value disables timeout policy via annotation.
     *
     * @return timeout in milliseconds
     */
    long timeoutMillis() default -1;
}
