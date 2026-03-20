package org.royada.flowforge.harness;

import org.royada.flowforge.spring.annotations.FlowTask;
import org.royada.flowforge.spring.annotations.TaskHandler;
import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@TaskHandler("stress-harness")
public class HarnessTaskHandlers {

    @FlowTask(id = "cpu1")
    public Mono<Integer> cpu1(Void input, ReactiveExecutionContext ctx) {
        return cpu(null);
    }

    @FlowTask(id = "cpu2")
    public Mono<Integer> cpu2(Integer input, ReactiveExecutionContext ctx) {
        return cpu(input);
    }

    @FlowTask(id = "cpu3")
    public Mono<Integer> cpu3(Integer input, ReactiveExecutionContext ctx) {
        return cpu(input);
    }

    @FlowTask(id = "cpu4")
    public Mono<Integer> cpu4(Integer input, ReactiveExecutionContext ctx) {
        return cpu(input);
    }

    @FlowTask(id = "cpu5")
    public Mono<Integer> cpu5(Integer input, ReactiveExecutionContext ctx) {
        return cpu(input);
    }

    @FlowTask(id = "cpu6")
    public Mono<Integer> cpu6(Integer input, ReactiveExecutionContext ctx) {
        return cpu(input);
    }

    @FlowTask(id = "nb1")
    public Mono<Integer> nb1(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb2")
    public Mono<Integer> nb2(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb3")
    public Mono<Integer> nb3(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb4")
    public Mono<Integer> nb4(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb5")
    public Mono<Integer> nb5(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb6")
    public Mono<Integer> nb6(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb7")
    public Mono<Integer> nb7(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb8")
    public Mono<Integer> nb8(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb9")
    public Mono<Integer> nb9(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb10")
    public Mono<Integer> nb10(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb11")
    public Mono<Integer> nb11(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb12")
    public Mono<Integer> nb12(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb13")
    public Mono<Integer> nb13(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb14")
    public Mono<Integer> nb14(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb15")
    public Mono<Integer> nb15(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb16")
    public Mono<Integer> nb16(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb17")
    public Mono<Integer> nb17(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb18")
    public Mono<Integer> nb18(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb19")
    public Mono<Integer> nb19(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb20")
    public Mono<Integer> nb20(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb21")
    public Mono<Integer> nb21(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb22")
    public Mono<Integer> nb22(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "nb23")
    public Mono<Integer> nb23(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlocking(input);
    }

    @FlowTask(id = "blk1")
    public Mono<Integer> blk1(Void input, ReactiveExecutionContext ctx) {
        return ioBlocking(null);
    }

    @FlowTask(id = "blk2")
    public Mono<Integer> blk2(Integer input, ReactiveExecutionContext ctx) {
        return ioBlocking(input);
    }

    @FlowTask(id = "finalize")
    public Mono<Integer> finalizeTask(Object input, ReactiveExecutionContext ctx) {
        return finalizeValue(input);
    }

    @FlowTask(id = "validateInput")
    public Mono<Integer> validateInput(Void input, ReactiveExecutionContext ctx) {
        return cpuLight(null);
    }

    @FlowTask(id = "profileService")
    public Mono<Integer> profileService(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlockingRange(input, 20, 60);
    }

    @FlowTask(id = "pricingService")
    public Mono<Integer> pricingService(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlockingRange(input, 30, 80);
    }

    @FlowTask(id = "riskService")
    public Mono<Integer> riskService(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlockingRange(input, 40, 120);
    }

    @FlowTask(id = "aggregateOffer")
    public Mono<Integer> aggregateOffer(Object input, ReactiveExecutionContext ctx) {
        return finalizeValue(input);
    }

    @FlowTask(id = "persistAudit")
    public Mono<Integer> persistAudit(Integer input, ReactiveExecutionContext ctx) {
        return ioNonBlockingRange(input, 20, 50);
    }

    private Mono<Integer> cpu(Integer input) {
        return Mono.fromSupplier(() -> {
            long x = 0;
            int n = input == null ? 50_000 : input;
            for (int i = 0; i < n; i++) {
                x = (x * 31) ^ i;
            }
            return (int) (x & 0x7fffffff);
        });
    }

    private Mono<Integer> ioNonBlocking(Integer input) {
        return ioNonBlockingRange(input, 5, 25);
    }

    private Mono<Integer> ioNonBlockingRange(Integer input, int minMsInclusive, int maxMsInclusive) {
        int ms = ThreadLocalRandom.current().nextInt(minMsInclusive, maxMsInclusive + 1);
        return Mono.delay(Duration.ofMillis(ms))
                .thenReturn(input == null ? 1 : input + 1);
    }

    private Mono<Integer> ioBlocking(Integer input) {
        int ms = ThreadLocalRandom.current().nextInt(5, 25);
        return Mono.fromCallable(() -> {
            Thread.sleep(ms);
            return input == null ? 1 : input + 1;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Integer> finalizeValue(Object input) {
        return Mono.fromSupplier(() -> {
            if (input == null) {
                return 0;
            }
            if (input instanceof Integer i) {
                return i;
            }
            if (input instanceof Map<?, ?> m) {
                int sum = 0;
                for (Object v : m.values()) {
                    if (v instanceof Number n) {
                        sum += n.intValue();
                    }
                }
                return sum;
            }
            return 0;
        });
    }

    private Mono<Integer> cpuLight(Integer input) {
        return Mono.fromSupplier(() -> {
            int n = input == null ? 4_000 : Math.max(1_000, Math.min(input, 12_000));
            int acc = 0;
            for (int i = 0; i < n; i++) {
                acc ^= (i * 31);
                acc = Integer.rotateLeft(acc, 1);
            }
            return Math.abs(acc);
        });
    }
}
