package core.global.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PresenceGaugeScheduler {

    private final StringRedisTemplate redis;
    private final PresenceMetrics presenceMetrics; // 기존 클래스 사용
    private static final String ZKEY = "presence:active:zset";

    @Scheduled(fixedDelay = 30000) // 30초마다
    public void refreshPresenceGauge() {
        long now = System.currentTimeMillis();

        redis.opsForZSet().removeRangeByScore(ZKEY, Double.NEGATIVE_INFINITY, now - 1);
        Long count = redis.opsForZSet().count(ZKEY, now, Double.POSITIVE_INFINITY);
        int online = (count == null ? 0 : count.intValue());

        presenceMetrics.setCurrentConnected(online);
    }
}
