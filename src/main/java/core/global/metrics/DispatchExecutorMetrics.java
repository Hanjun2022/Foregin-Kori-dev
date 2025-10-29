package core.global.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class DispatchExecutorMetrics {

    private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
    private final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(10, 50, 60, TimeUnit.SECONDS, queue);

    public DispatchExecutorMetrics(MeterRegistry registry) {
        Gauge.builder("dispatch_queue_size", queue, ArrayBlockingQueue::size)
                .description("발송 큐 현재 크기").register(registry);
        Gauge.builder("dispatch_queue_capacity", () -> 1000)
                .description("발송 큐 용량").register(registry);
        Gauge.builder("dispatch_executor_active_threads", executor, ThreadPoolExecutor::getActiveCount)
                .description("활성 스레드 수").register(registry);
        Gauge.builder("dispatch_executor_pool_size", executor, ThreadPoolExecutor::getPoolSize)
                .description("풀 사이즈").register(registry);
    }

    public void submit(Runnable r) { executor.submit(r); }
}
