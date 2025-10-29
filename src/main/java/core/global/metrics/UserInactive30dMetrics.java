package core.global.metrics;

import core.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class UserInactive30dMetrics {

    private final UserRepository userRepository;

    private final AtomicInteger inactive30d = new AtomicInteger(0);
    private final AtomicInteger totalUsers  = new AtomicInteger(0);

    public UserInactive30dMetrics(MeterRegistry registry, UserRepository userRepository) {
        this.userRepository = userRepository;

        log.info("[U30d] MeterRegistry = {}", registry.getClass().getName());

        Gauge.builder("app_user_inactive_30d", inactive30d, AtomicInteger::get)
                .description("Users inactive for 30d+")
                .register(registry);

        Gauge.builder("app_users_count", totalUsers, AtomicInteger::get)
                .description("Total users")
                .register(registry);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        log.info("[U30d] warmup start");
        try {
            collect();
        } catch (Exception e) {
            log.warn("[U30d] warmup failed", e);
        }
        inactive30d.get();
        totalUsers.get();
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT30S")
    @Transactional(readOnly = true)
    public void collect() {
        try {
            Object res = userRepository.countInactive30dAndTotal();
            Object[] row = toRow(res);
            if (row.length < 2) {
                log.warn("[U30d] unexpected row size: {}", row.length);
                return;
            }
            int ina = toInt(row[0]);
            int tot = toInt(row[1]);

            inactive30d.set(ina);
            totalUsers.set(tot);

            log.info("[U30d] set inactive30d={}, totalUsers={}", ina, tot);
        } catch (Exception e) {
            log.error("[U30d] collect failed", e);
        }
    }

    private Object[] toRow(Object res) {
        if (res == null) return new Object[0];
        if (res instanceof Object[] a) return a;
        if (res instanceof Collection<?> c) {
            if (c.isEmpty()) return new Object[0];
            Object first = c.iterator().next();
            return first instanceof Object[] arr ? arr : new Object[]{ first };
        }
        return new Object[]{ res };
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); }
        catch (Exception ignore) { return 0; }
    }
}