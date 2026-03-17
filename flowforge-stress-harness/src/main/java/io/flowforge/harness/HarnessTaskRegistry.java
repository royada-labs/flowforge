package io.flowforge.harness;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.flowforge.spring.annotations.FlowTask;
import io.flowforge.api.FlowTaskHandler;

@Configuration
public class HarnessTaskRegistry {

    // CPU tasks (IDs distintos, mismo handler)
    @Bean
    @FlowTask(id = "cpu1")
    FlowTaskHandler<Integer, Integer> cpu1() {
        return TaskHandlers.cpu();
    }

    @Bean
    @FlowTask(id = "cpu2")
    FlowTaskHandler<Integer, Integer> cpu2() {
        return TaskHandlers.cpu();
    }

    @Bean
    @FlowTask(id = "cpu3")
    FlowTaskHandler<Integer, Integer> cpu3() {
        return TaskHandlers.cpu();
    }

    @Bean
    @FlowTask(id = "cpu4")
    FlowTaskHandler<Integer, Integer> cpu4() {
        return TaskHandlers.cpu();
    }

    @Bean
    @FlowTask(id = "cpu5")
    FlowTaskHandler<Integer, Integer> cpu5() {
        return TaskHandlers.cpu();
    }

    @Bean
    @FlowTask(id = "cpu6")
    FlowTaskHandler<Integer, Integer> cpu6() {
        return TaskHandlers.cpu();
    }

    // Non-blocking IO tasks (IDs distintos, mismo handler)
    @Bean
    @FlowTask(id = "nb1")
    FlowTaskHandler<Integer, Integer> nb1() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb2")
    FlowTaskHandler<Integer, Integer> nb2() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb3")
    FlowTaskHandler<Integer, Integer> nb3() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb4")
    FlowTaskHandler<Integer, Integer> nb4() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb5")
    FlowTaskHandler<Integer, Integer> nb5() {
        return TaskHandlers.ioNonBlocking();
    }

    // Para SEQ_M (18 pasos sin repetir IDs)
    @Bean
    @FlowTask(id = "nb6")
    FlowTaskHandler<Integer, Integer> nb6() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb7")
    FlowTaskHandler<Integer, Integer> nb7() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb8")
    FlowTaskHandler<Integer, Integer> nb8() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb9")
    FlowTaskHandler<Integer, Integer> nb9() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb10")
    FlowTaskHandler<Integer, Integer> nb10() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb11")
    FlowTaskHandler<Integer, Integer> nb11() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb12")
    FlowTaskHandler<Integer, Integer> nb12() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb13")
    FlowTaskHandler<Integer, Integer> nb13() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb14")
    FlowTaskHandler<Integer, Integer> nb14() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb15")
    FlowTaskHandler<Integer, Integer> nb15() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb16")
    FlowTaskHandler<Integer, Integer> nb16() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb17")
    FlowTaskHandler<Integer, Integer> nb17() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb18")
    FlowTaskHandler<Integer, Integer> nb18() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb19")
    FlowTaskHandler<Integer, Integer> nb19() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb20")
    FlowTaskHandler<Integer, Integer> nb20() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb21")
    FlowTaskHandler<Integer, Integer> nb21() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb22")
    FlowTaskHandler<Integer, Integer> nb22() {
        return TaskHandlers.ioNonBlocking();
    }

    @Bean
    @FlowTask(id = "nb23")
    FlowTaskHandler<Integer, Integer> nb23() {
        return TaskHandlers.ioNonBlocking();
    }

    // Blocking IO tasks (IDs distintos, mismo handler)
    @Bean
    @FlowTask(id = "blk1")
    FlowTaskHandler<Integer, Integer> blk1() {
        return TaskHandlers.ioBlocking();
    }

    @Bean
    @FlowTask(id = "blk2")
    FlowTaskHandler<Integer, Integer> blk2() {
        return TaskHandlers.ioBlocking();
    }

    // Finalize
    @Bean
    @FlowTask(id = "finalize")
    FlowTaskHandler<Object, Integer> finalizeTask() {
        return TaskHandlers.finalizeTask();
    }
}
