package core.global.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final ThreadPoolTaskExecutor dispatchExecutor;

    public AsyncConfig(@Qualifier("dispatchExecutor") ThreadPoolTaskExecutor dispatchExecutor) {
        this.dispatchExecutor = dispatchExecutor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return dispatchExecutor; // ✅ @Async 기본 실행기 지정
    }

    // 선택: 비동기 예외 로깅 커스터마이즈 가능
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // log.warn("Async error in {}: {}", method, ex.getMessage(), ex);
        };
    }
}
