package core.global.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class UserActivityRecorder {
    private final StringRedisTemplate redis;

    public void recordActiveUser(String userId) {
        String dayKey = "hll:active:day:" + LocalDate.now();
        String monthKey = "hll:active:month:" + YearMonth.now();
        redis.opsForHyperLogLog().add(dayKey, userId);
        redis.opsForHyperLogLog().add(monthKey, userId);
    }
}

