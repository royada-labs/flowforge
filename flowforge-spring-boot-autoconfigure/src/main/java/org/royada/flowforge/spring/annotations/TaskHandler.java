package org.royada.flowforge.spring.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring bean class as a container of {@link FlowTask}-annotated methods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TaskHandler {
    /**
     * Optional logical group tag for documentation and tooling.
     *
     * @return logical group name
     */
    String value() default "";
}

