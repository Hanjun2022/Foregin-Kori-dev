package core.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class MailAsyncConfig {
    @Bean("mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);          // NAVER SMTP는 동시접속 1~2가 안전
        exec.setMaxPoolSize(1);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("mail-");
        exec.initialize();
        return exec;
    }
}