package core.global.metrics;

import core.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class UserPeakHourMetrics {

    private final UserRepository userRepository;
    private final MultiGauge peakGauge; // user_last_seen_hour{hour="0..23"}

    private final Map<Integer, AtomicInteger> buckets =
            new java.util.concurrent.ConcurrentHashMap<>();

    public UserPeakHourMetrics(UserRepository userRepository, MeterRegistry registry) {
        this.userRepository = userRepository;
        this.peakGauge = MultiGauge.builder("user_last_seen_hour")
                .description("Last seen distribution by hour of day (last 7d)")
                .register(registry);

        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            AtomicInteger ref = buckets.computeIfAbsent(h, k -> new AtomicInteger(0));
            rows.add(MultiGauge.Row.of(
                    Tags.of("hour", String.format("%02d", h)), // "00".."23"
                    ref,
                    ai -> (double) ai.get()
            ));

        }
        peakGauge.register(rows, true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() { collect(); }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    @Transactional(readOnly = true)
    public void collect() {
        buckets.values().forEach(ai -> ai.set(0));

        for (Object[] r : userRepository.lastSeenHourDist7d()) {
            int hour = ((Number) r[0]).intValue();  // 0~23
            int cnt  = ((Number) r[1]).intValue();
            AtomicInteger bucket = buckets.get(hour);
            if (bucket != null) bucket.set(cnt);
        }
    }
}