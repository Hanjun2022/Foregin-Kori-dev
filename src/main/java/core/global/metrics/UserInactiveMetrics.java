package core.global.metrics;

import core.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserInactiveMetrics {

    private final UserRepository userRepository;
    private final MultiGauge inactiveGauge;

    public UserInactiveMetrics(MeterRegistry registry, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.inactiveGauge = MultiGauge.builder("user_inactivity_hours")
                .description("Number of users by inactivity buckets (hours since lastSeenAt)")
                .register(registry);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        safeCollect();
    }

    /**
     * 2분마다 갱신
     */
    @Scheduled(fixedDelayString = "PT2M")
    public void scheduled() {
        safeCollect();
    }

    private void safeCollect() {
        try {
            Object raw = userRepository.countInactiveBuckets();

            Object[] row;

            if (raw instanceof Object[] arr) {
                if (arr.length == 1 && arr[0] instanceof Object[] inner) {
                    row = inner;
                } else if (arr.length > 0 && !(arr[0] instanceof Object)) {
                    row = arr;
                } else {
                    row = arr;
                }
            } else if (raw instanceof java.util.Collection<?> col) {
                if (col.isEmpty()) {
                    log.warn("countInactiveBuckets() returned empty collection");
                    return;
                }
                Object first = col.iterator().next();
                if (!(first instanceof Object[] inner)) {
                    log.warn("Unexpected element type: {}", first == null ? "null" : first.getClass());
                    return;
                }
                row = inner;
            } else {
                log.warn("countInactiveBuckets() returned unexpected: {}", raw == null ? "null" : raw.getClass());
                return;
            }

            double[] nums = new double[row.length];
            for (int i = 0; i < row.length; i++) {
                Object v = row[i];
                if (v == null) {
                    nums[i] = 0d;
                } else if (v instanceof Number n) {
                    nums[i] = n.doubleValue();
                } else if (v instanceof Object[] nested && nested.length > 0 && nested[0] instanceof Number n0) {
                    nums[i] = n0.doubleValue();
                } else {
                    nums[i] = Double.parseDouble(String.valueOf(v));
                }
            }

            inactiveGauge.register(
                    java.util.List.of(
                            MultiGauge.Row.of(Tags.of("le", "24", "order", "024"), nums[0]),
                            MultiGauge.Row.of(Tags.of("le", "72", "order", "072"), nums[1]),
                            MultiGauge.Row.of(Tags.of("le", "168", "order", "168"), nums[2]),
                            MultiGauge.Row.of(Tags.of("le", "336", "order", "336"), nums[3]),
                            MultiGauge.Row.of(Tags.of("le", "720", "order", "720"), nums[4]),
                            MultiGauge.Row.of(Tags.of("le", "+Inf", "order", "999"), nums[5])
                    ),
                    true
            );


            log.debug("user_inactivity_hours updated: {}", java.util.Arrays.toString(nums));
        } catch (Exception e) {
            log.error("Failed to update user_inactivity_hours", e);
        }
    }

}
