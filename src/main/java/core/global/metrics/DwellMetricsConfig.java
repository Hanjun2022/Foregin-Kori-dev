package core.global.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DwellMetricsConfig {

    @Bean
    public MeterFilter dwellHistogramConfig() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if ("feature_dwell_seconds".equals(id.getName())
                    || "chat_room_dwell_seconds".equals(id.getName())) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .serviceLevelObjectives(
                                    1.0, 3.0, 5.0, 10.0, 15.0,
                                    30.0, 60.0, 120.0, 180.0, 300.0, 600.0
                            )
                            .minimumExpectedValue(0.001)
                            .maximumExpectedValue(3600.0)
                            .build().merge(config);
                }
                return config;
            }
        };
    }
}
