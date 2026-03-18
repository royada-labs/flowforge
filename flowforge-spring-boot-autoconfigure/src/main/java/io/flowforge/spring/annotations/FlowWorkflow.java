package io.flowforge.spring.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowWorkflow {

    /**
     * Identificador único del workflow.
     * Debe ser único en el ApplicationContext.
     */
    String id();

    /**
     * Descripción opcional (observabilidad / tooling).
     */
    String description() default "";
}