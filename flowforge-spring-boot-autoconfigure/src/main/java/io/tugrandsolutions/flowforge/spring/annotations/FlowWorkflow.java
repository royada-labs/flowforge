package io.tugrandsolutions.flowforge.spring.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
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