package core.global.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class UserMetrics {

    private final AtomicInteger dau = new AtomicInteger(0);
    private final AtomicInteger wau = new AtomicInteger(0);
    private final AtomicInteger mau = new AtomicInteger(0);
    private final AtomicInteger acu = new AtomicInteger(0);
    private final AtomicInteger mcu = new AtomicInteger(0);

    public UserMetrics(MeterRegistry registry) {
        Gauge.builder("app_users_dau", dau, AtomicInteger::get).register(registry);
        Gauge.builder("app_users_wau", wau, AtomicInteger::get).register(registry);
        Gauge.builder("app_users_mau", mau, AtomicInteger::get).register(registry);
        Gauge.builder("app_users_acu", acu, AtomicInteger::get).register(registry);
        Gauge.builder("app_users_mcu", mcu, AtomicInteger::get).register(registry);
    }

    public void update(int daily, int weekly, int monthly, int avgConcurrent, int maxConcurrent) {
        dau.set(daily);
        wau.set(weekly);
        mau.set(monthly);
        acu.set(avgConcurrent);
        mcu.set(maxConcurrent);
    }
}

