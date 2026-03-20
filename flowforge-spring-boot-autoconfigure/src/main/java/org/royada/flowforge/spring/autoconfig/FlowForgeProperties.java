package org.royada.flowforge.spring.autoconfig;

import org.royada.flowforge.workflow.orchestrator.BackpressureStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration root for FlowForge Spring Boot integration.
 */
@Data
@ConfigurationProperties(prefix = "flowforge")
public class FlowForgeProperties {
    /**
     * Creates property root with default sections.
     */
    public FlowForgeProperties() {
    }

    /**
     * Monitor-related settings.
     */
    private MonitorProperties monitor = new MonitorProperties();

    /**
     * Orchestrator execution limits and backpressure settings.
     */
    private ExecutionProperties execution = new ExecutionProperties();

    /**
     * Tracing configuration section.
     */
    private TracingProperties tracing = new TracingProperties();

    /**
     * Monitoring configuration values.
     */
    @Data
    public static class MonitorProperties {
        /**
         * Creates monitor properties with defaults.
         */
        public MonitorProperties() {
        }

        /**
         * Enables monitor registration and monitor callbacks.
         */
        private boolean enabled = true;
    }

    /**
     * Execution constraints for the reactive orchestrator.
     */
    @Data
    public static class ExecutionProperties {
        /**
         * Creates execution properties with defaults.
         */
        public ExecutionProperties() {
        }

        /**
         * Maximum number of tasks executing concurrently.
         */
        private int maxInFlightTasks = Math.max(2, Runtime.getRuntime().availableProcessors());

        /**
         * Maximum number of queued tasks before backpressure is applied.
         */
        private int maxQueueSize = 1000;

        /**
         * Strategy applied when queue or concurrency limits are reached.
         */
        private BackpressureStrategy backpressureStrategy = BackpressureStrategy.BLOCK;
    }

    /**
     * Tracing-related properties.
     */
    @Data
    public static class TracingProperties {
        /**
         * Creates tracing properties with defaults.
         */
        public TracingProperties() {
        }

        /**
         * OpenTelemetry tracing configuration.
         */
        private OpenTelemetryProperties opentelemetry = new OpenTelemetryProperties();
    }

    /**
     * OpenTelemetry feature flags.
     */
    @Data
    public static class OpenTelemetryProperties {
        /**
         * Creates OpenTelemetry properties with defaults.
         */
        public OpenTelemetryProperties() {
        }

        /**
         * Enables the OpenTelemetry execution tracer auto-configuration.
         */
        private boolean enabled = false;
    }
}
