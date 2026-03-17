package io.flowforge.spring.annotations;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowTask {
    String id();
    boolean optional() default false;
    String[] dependsOn() default {};
}