package core.global.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {
    @Bean
    public ThreadPoolTaskExecutor dispatchExecutor(MeterRegistry registry) {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(10);
        exec.setMaxPoolSize(50);
        exec.setQueueCapacity(1000);
        exec.setKeepAliveSeconds(60);
        exec.setThreadNamePrefix("dispatch-");
        exec.initialize();

        // Micrometer 바인딩 (executor_* 표준 메트릭 세트)
//        ExecutorServiceMetrics.monitor(registry, exec.getThreadPoolExecutor(), "dispatch");
        return exec;
    }
}
