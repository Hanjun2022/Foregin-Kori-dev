package core.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class FeatureUsageMetrics {

    private final MeterRegistry registry;

    private final Counter chatFeatureCounter;
    private final Counter followFeatureCounter;
    private final Counter communityFeatureCounter;

    public FeatureUsageMetrics(MeterRegistry registry) {
        this.registry = registry;
        chatFeatureCounter = Counter.builder("feature_chat_usage_count")
                .description("채팅 기능 사용 횟수")
                .register(registry);
        followFeatureCounter = Counter.builder("feature_follow_usage_count")
                .description("팔로우 기능 사용 횟수")
                .register(registry);

        communityFeatureCounter = Counter.builder("feature_community_usage_count")
                .description("커뮤니티 기능 사용 횟수")
                .register(registry);
    }

    private void incTotalByFeature(String feature) {
        registry.counter("feature_usage_total", Tags.of("feature", feature)).increment();
    }

    public void recordChatUsage() {
        chatFeatureCounter.increment();
        incTotalByFeature("chat");
    }

    public void recordFollowUsage() {
        followFeatureCounter.increment();
        incTotalByFeature("follow");
    }

    public void recordCommunityUsage() {
        communityFeatureCounter.increment();
        incTotalByFeature("community");
    }
}