package io.tugrandsolutions.flowforge.spring.autoconfig;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.tugrandsolutions.flowforge.workflow.orchestrator.ReactiveWorkflowOrchestrator;
import io.tugrandsolutions.flowforge.workflow.trace.ExecutionTracerFactory;
import io.tugrandsolutions.flowforge.workflow.trace.OpenTelemetryExecutionTracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FlowForgeOpenTelemetryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    FlowForgeAutoConfiguration.class,
                    FlowForgeOpenTelemetryAutoConfiguration.class));

    @Test
    void shouldNotProvideTracerIfPropertyDisabled() {
        contextRunner
                .withUserConfiguration(OpenTelemetryMockConfiguration.class)
                .withPropertyValues("flowforge.tracing.opentelemetry.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ExecutionTracerFactory.class);
                });
    }

    @Test
    void shouldProvideTracerIfPropertyEnabledAndOpenTelemetryPresent() {
        contextRunner
                .withUserConfiguration(OpenTelemetryMockConfiguration.class)
                .withPropertyValues("flowforge.tracing.opentelemetry.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ExecutionTracerFactory.class);
                    assertThat(context).hasSingleBean(ReactiveWorkflowOrchestrator.class);
                    
                    ExecutionTracerFactory factory = context.getBean(ExecutionTracerFactory.class);
                    // Verify it produces OTel tracers
                    assertThat(factory.create(java.util.Collections.emptyMap()))
                            .isInstanceOf(OpenTelemetryExecutionTracer.class);
                });
    }

    @Configuration
    static class OpenTelemetryMockConfiguration {
        @Bean
        public OpenTelemetry openTelemetry() {
            OpenTelemetry otel = mock(OpenTelemetry.class);
            Tracer tracer = mock(Tracer.class);
            org.mockito.Mockito.when(otel.getTracer(org.mockito.Mockito.anyString())).thenReturn(tracer);
            return otel;
        }
    }
}
