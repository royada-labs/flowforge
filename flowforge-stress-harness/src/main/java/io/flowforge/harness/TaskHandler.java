package io.flowforge.harness;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import io.flowforge.api.FlowTaskHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

final class TaskHandlers {

    private TaskHandlers() {
    }

    static FlowTaskHandler<Integer, Integer> cpu() {
        return (input, ctx) -> Mono.fromSupplier(() -> {
            long x = 0;
            int n = input == null ? 50_000 : input;
            for (int i = 0; i < n; i++) {
                x = (x * 31) ^ i;
            }
            return (int) (x & 0x7fffffff);
        });
    }

    static FlowTaskHandler<Integer, Integer> ioNonBlocking() {
        return (input, ctx) -> {
            int ms = ThreadLocalRandom.current().nextInt(5, 25);
            return Mono.delay(Duration.ofMillis(ms))
                    .thenReturn(input == null ? 1 : input + 1);
        };
    }

    static FlowTaskHandler<Integer, Integer> ioBlocking() {
        return (input, ctx) -> {
            int ms = ThreadLocalRandom.current().nextInt(5, 25);
            return Mono.fromCallable(() -> {
                Thread.sleep(ms);
                return input == null ? 1 : input + 1;
            })
                    .subscribeOn(Schedulers.boundedElastic());
        };
    }

    static FlowTaskHandler<Object, Integer> finalizeTask() {
        return (input, ctx) -> Mono.fromSupplier(() -> {
            if (input == null)
                return 0;
            if (input instanceof Integer i)
                return i;
            if (input instanceof Map<?, ?> m) {
                int sum = 0;
                for (Object v : m.values()) {
                    if (v instanceof Number n)
                        sum += n.intValue();
                }
                return sum;
            }
            return 0;
        });
    }

}
