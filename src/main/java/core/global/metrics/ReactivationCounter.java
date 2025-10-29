package core.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ReactivationCounter {

    private final Counter counter;

    public ReactivationCounter(MeterRegistry registry) {
        this.counter = Counter.builder("user_reactivated_total")
                .description("Users who came back after 30d+ dormancy")
                .register(registry);
    }
    public void inc() { counter.increment(); }
}
