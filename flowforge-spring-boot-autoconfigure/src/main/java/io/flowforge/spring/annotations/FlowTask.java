package io.flowforge.spring.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlowTask {
    String id();
    boolean optional() default false;
    String[] dependsOn() default {};
    Class<?> inputType() default Object.class;
    Class<?> outputType() default Object.class;
    int retryMaxRetries() default -1;
    long retryBackoffMillis() default -1;
    long timeoutMillis() default -1;
}
