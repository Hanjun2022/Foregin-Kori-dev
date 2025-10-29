package core.global.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatRoomDwellRecorder {

    private final MeterRegistry registry;
    private final StringRedisTemplate redis;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ConcurrentHashMap<String, Long> startedAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roomIdMap = new ConcurrentHashMap<>();

    private final DistributionSummary dwellSeconds;

    public ChatRoomDwellRecorder(MeterRegistry registry, StringRedisTemplate redis) {
        this.registry = registry;
        this.redis = redis;
        this.dwellSeconds = DistributionSummary.builder("chat_room_dwell_seconds")
                .description("채팅방 체류 시간(초) 분포 (전체 chat)")
                .serviceLevelObjectives(1, 3, 5, 10, 15, 30, 60, 120, 180, 300, 600)
                .maximumExpectedValue(3600.0)
                .publishPercentileHistogram()
                .register(registry);
    }

    /** 방 입장(예: STOMP SUBSCRIBE) */
    public void onEnter(String sessionId, String roomId) {
        long now = System.currentTimeMillis();
        startedAt.put(sessionId, now);
        roomIdMap.put(sessionId, nz(roomId));
    }

    /** 방 이탈(예: DISCONNECT/UNSUBSCRIBE) */
    public void onLeave(String sessionId) {
        Long start = startedAt.remove(sessionId);
        String roomId = roomIdMap.remove(sessionId);
        if (start == null || roomId == null) return;

        long durMs = Math.max(0, System.currentTimeMillis() - start);
        double seconds = durMs / 1000.0;

        // 분포 기록(태그 없음, 전체 chat 단일 시계열)
        dwellSeconds.record(seconds);

        // Top-N: 일별 roomId 누적 체류초
        String zkey = "chat:dwell:" + DAY_FMT.format(LocalDate.now());
        redis.opsForZSet().incrementScore(zkey, roomId, seconds);
        // 세션 수도 함께 보려면 유지
        redis.opsForZSet().incrementScore(zkey + ":sessions", roomId, 1.0);
    }

    private static String nz(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s;
    }
}
