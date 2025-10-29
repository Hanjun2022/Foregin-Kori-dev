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

@Component
public class UserCohortMetrics {

    private final UserRepository userRepository;
    private final MultiGauge cohortGauge; // user_cohort_retention{cohort_week="YYYY-MM-DD"}

    public UserCohortMetrics(UserRepository userRepository, MeterRegistry registry) {
        this.userRepository = userRepository;
        this.cohortGauge = MultiGauge.builder("user_cohort_retention")
                .description("30d retention by cohort week")
                .register(registry);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() { refresh(); }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    @Transactional(readOnly = true)
    public void refresh() {
        List<Object[]> rows = userRepository.cohortRetention30d();
        List<MultiGauge.Row<?>> out = new ArrayList<>();
        for (Object[] r : rows) {
            String week = String.valueOf(r[0]);      // "2025-10-06"
            double total = ((Number) r[1]).doubleValue();
            double active = ((Number) r[2]).doubleValue();
            double rate = total > 0 ? active / total : 0.0;
            out.add(MultiGauge.Row.of(Tags.of("cohort_week", week), rate));
        }
        cohortGauge.register(out, true);
    }
}
