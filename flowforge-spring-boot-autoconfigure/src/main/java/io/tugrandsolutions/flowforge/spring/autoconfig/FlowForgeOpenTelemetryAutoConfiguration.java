package io.tugrandsolutions.flowforge.spring.autoconfig;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.tugrandsolutions.flowforge.workflow.trace.ExecutionTracerFactory;
import io.tugrandsolutions.flowforge.workflow.trace.OpenTelemetryExecutionTracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenTelemetry tracing in FlowForge.
 */
@AutoConfiguration(afterName = "io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration")
@ConditionalOnClass({OpenTelemetry.class, Tracer.class})
@ConditionalOnProperty(name = "flowforge.tracing.opentelemetry.enabled", havingValue = "true")
public class FlowForgeOpenTelemetryAutoConfiguration {

    @Bean
    @ConditionalOnBean(OpenTelemetry.class)
    @ConditionalOnMissingBean(ExecutionTracerFactory.class)
    public ExecutionTracerFactory openTelemetryExecutionTracerFactory(OpenTelemetry openTelemetry) {
        Tracer tracer = openTelemetry.getTracer("io.tugrandsolutions.flowforge");
        return typeMetadata -> new OpenTelemetryExecutionTracer(tracer, typeMetadata);
    }
}
