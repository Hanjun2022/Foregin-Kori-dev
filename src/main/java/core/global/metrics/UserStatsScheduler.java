package core.global.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.IntStream;


@Component
@RequiredArgsConstructor
public class UserStatsScheduler {

    private static final boolean USE_HOURLY_FOR_DAU = false;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH");
    private final StringRedisTemplate redis;
    private final UserMetrics userMetrics;

    @Scheduled(cron = "0 */5 * * * *")
    public void refreshUserMetrics() {
        long dau;
        if (USE_HOURLY_FOR_DAU) {
            String[] last24HourKeys = IntStream.rangeClosed(0, 23)
                    .mapToObj(i -> "hll:active:hour:" + HOUR_FMT.format(LocalDateTime.now().minusHours(i)))
                    .toArray(String[]::new);
            Long v = redis.opsForHyperLogLog().size(last24HourKeys);
            dau = v != null ? v : 0L;
        } else {
            String dayKey = "hll:active:day:" + DAY_FMT.format(LocalDate.now());
            Long v = redis.opsForHyperLogLog().size(dayKey);
            dau = v != null ? v : 0L;
        }

        String[] last7Day = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> "hll:active:day:" + DAY_FMT.format(LocalDate.now().minusDays(i)))
                .toArray(String[]::new);
        int wau = Optional.of(redis.opsForHyperLogLog().size(last7Day)).orElse(0L).intValue();

        String[] last30Day = IntStream.rangeClosed(0, 29)
                .mapToObj(i -> "hll:active:day:" + DAY_FMT.format(LocalDate.now().minusDays(i)))
                .toArray(String[]::new);
        int mau = Optional.of(redis.opsForHyperLogLog().size(last30Day)).orElse(0L).intValue();

        userMetrics.update((int) dau, wau, mau, 0, 0);
    }
}
