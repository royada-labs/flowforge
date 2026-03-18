package io.flowforge.harness;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.HdrHistogram.ConcurrentHistogram;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.flowforge.api.FlowForgeClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Component
public class StressRunner implements CommandLineRunner {

    private final FlowForgeClient flowForge;
    private final MeterRegistry registry;

    private final AtomicInteger inflight = new AtomicInteger(0);
    private final ConcurrentHistogram globalHist = new ConcurrentHistogram(3);
    private final Map<String, ConcurrentHistogram> perScenario = new ConcurrentHashMap<>();

    public StressRunner(FlowForgeClient flowForge, MeterRegistry registry) {
        this.flowForge = flowForge;
        this.registry = registry;
        // Global metric gauge (only once)
        registry.gauge("flowforge.harness.inflight", inflight);
    }

    @Override
    public void run(String... args) {
        // Escenarios default (closed-loop por inflight).
        // Ajusta inflight/duración a tu hardware.
        runScenario("LOW", 200, Duration.ofMinutes(2), "SEQ_S", Duration.ofSeconds(60),
            Schedulers.boundedElastic());
        runScenario("MID", 200, Duration.ofMinutes(3), "FORK_4_JOIN", Duration.ofSeconds(120),
            Schedulers.boundedElastic());
        runScenario("MID_REALISTIC", 60, Duration.ofMinutes(2), "MID_REALISTIC", Duration.ofSeconds(2),
            Schedulers.boundedElastic());
        runScenario("HIGH", 3000, Duration.ofMinutes(3), "BLOCKING_MIX", Duration.ofSeconds(10),
            Schedulers.parallel());

        perScenario.forEach(this::printSummary);
        printSummary("GLOBAL", globalHist);

        System.out.println("\nPrometheus: http://localhost:8090/actuator/prometheus");
        System.exit(0);
    }

    private void runScenario(String scenario, int targetInflight, Duration duration, String workflowId,
            Duration timeout, Scheduler scheduler) {
        System.out.printf(
                "\n=== Scenario %s | workflow=%s | targetInflight=%d | duration=%s | timeout=%s ===\n",
                scenario, workflowId, targetInflight, duration, timeout);

        // Métrica Micrometer para scrapeo
        Timer wfTimer = Timer.builder("flowforge.harness.workflow.duration")
                .tag("scenario", scenario)
                .tag("workflow", workflowId)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        ConcurrentHistogram hist = perScenario.computeIfAbsent(
                scenario + ":" + workflowId,
                k -> new ConcurrentHistogram(3));

        long endAt = System.nanoTime() + duration.toNanos();

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        // Error breakdown
        Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();

        while (System.nanoTime() < endAt) {
            // Closed-loop: mantener targetInflight ejecuciones simultáneas
            while (inflight.get() < targetInflight && System.nanoTime() < endAt) {
                inflight.incrementAndGet();
                long start = System.nanoTime();
                // Typed DSL roots in this harness are Void-based.
                // Scenario variability is modeled inside task handlers.
                Object executionInput = null;

                Mono<Void> exec = flowForge.execute(workflowId, executionInput)
                        .timeout(timeout) // configurable
                        .then();

                exec.subscribeOn(scheduler) // configurable
                        .doOnSuccess(v -> {
                            long durNs = System.nanoTime() - start;
                            wfTimer.record(durNs, TimeUnit.NANOSECONDS);
                            hist.recordValue(durNs);
                            globalHist.recordValue(durNs);
                            ok.incrementAndGet();
                        })
                        .doOnError(e -> {
                            long durNs = System.nanoTime() - start;
                            // Record latency for failures too
                            hist.recordValue(durNs);
                            globalHist.recordValue(durNs);
                            fail.incrementAndGet();
                            errorCounts.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicInteger())
                                    .incrementAndGet();
                        })
                        .doFinally(sig -> inflight.decrementAndGet())
                        .subscribe(v -> {
                        }, e -> {
                        });
            }

            // Evita busy-wait
            sleepQuietly(5);
        }

        // drenar
        while (inflight.get() > 0) {
            sleepQuietly(50);
        }

        double seconds = duration.toMillis() / 1000.0;
        double throughput = ok.get() / seconds;

        System.out.printf(
                "Done %s: ok=%d fail=%d throughput=%.1f wf/s\n",
                scenario, ok.get(), fail.get(), throughput);

        if (fail.get() > 0) {
            System.out.println("  -> Errors breakdown: " + errorCounts);
        }

        printSummary(scenario + ":" + workflowId, hist);
    }

    private void printSummary(String key, ConcurrentHistogram h) {
        // Guardamos nanosegundos; imprimimos ms
        double p50 = h.getValueAtPercentile(50.0) / 1_000_000.0;
        double p95 = h.getValueAtPercentile(95.0) / 1_000_000.0;
        double p99 = h.getValueAtPercentile(99.0) / 1_000_000.0;
        double p999 = h.getValueAtPercentile(99.9) / 1_000_000.0;

        System.out.printf(
                "[%s] latency_ms p50=%.3f p95=%.3f p99=%.3f p99.9=%.3f (n=%d)\n",
                key, p50, p95, p99, p999, h.getTotalCount());
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
