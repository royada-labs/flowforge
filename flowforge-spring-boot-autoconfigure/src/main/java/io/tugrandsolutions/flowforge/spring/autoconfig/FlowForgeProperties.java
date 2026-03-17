package io.tugrandsolutions.flowforge.spring.autoconfig;

import io.tugrandsolutions.flowforge.workflow.orchestrator.BackpressureStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "flowforge")
public class FlowForgeProperties {
    private MonitorProperties monitor = new MonitorProperties();
    private ExecutionProperties execution = new ExecutionProperties();
    private TracingProperties tracing = new TracingProperties();

    @Data
    public static class MonitorProperties {
        private boolean enabled = true;
    }

    @Data
    public static class ExecutionProperties {
        private int maxInFlightTasks = Math.max(2, Runtime.getRuntime().availableProcessors());
        private int maxQueueSize = 1000;
        private BackpressureStrategy backpressureStrategy = BackpressureStrategy.BLOCK;
    }

    @Data
    public static class TracingProperties {
        private OpenTelemetryProperties opentelemetry = new OpenTelemetryProperties();
    }

    @Data
    public static class OpenTelemetryProperties {
        private boolean enabled = false;
    }
}
