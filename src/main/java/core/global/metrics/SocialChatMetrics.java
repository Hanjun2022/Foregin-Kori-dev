package core.global.metrics;

import core.global.Country;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class SocialChatMetrics {

    private static final Map<String, String> LOWER_TO_CANON =
            Country.ALL.stream().collect(Collectors.toUnmodifiableMap(
                    s -> s.toLowerCase(), s -> s
            ));
    private final MeterRegistry registry;
    private final Counter followCreateCount;
    private final Counter unfollowCount;
    private final Counter chatMessageCount;
    private final Counter chatRoomCreateCount;
    private final DistributionSummary chatMessageBytes; // 메시지 크기 분포
    private final AtomicInteger activeChatRooms = new AtomicInteger(0);
    private final AtomicInteger totalChatRooms = new AtomicInteger(0);
    private final AtomicInteger activeFollowEdges = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Boolean> roomActiveMap = new ConcurrentHashMap<>();

    public SocialChatMetrics(MeterRegistry registry) {
        this.registry = registry;
        followCreateCount = Counter.builder("feature_follow_create_total")
                .description("팔로우 생성 누계").register(registry);
        unfollowCount = Counter.builder("feature_unfollow_total")
                .description("언팔로우 누계").register(registry);
        chatMessageCount = Counter.builder("feature_chat_message_total")
                .description("채팅 메시지 수 누계").register(registry);
        chatRoomCreateCount = Counter.builder("feature_chat_room_create_total")
                .description("채팅방 생성 누계").register(registry);

        chatMessageBytes = DistributionSummary.builder("feature_chat_message_bytes")
                .description("채팅 메시지 페이로드 바이트 분포")
                .baseUnit("bytes")
                .publishPercentileHistogram()
                .maximumExpectedValue(1_000_000.0) // 필요시 조정
                .register(registry);

        Gauge.builder("chat_rooms_active", activeChatRooms, AtomicInteger::get)
                .description("활성 채팅방 수").register(registry);
        Gauge.builder("chat_rooms_total", totalChatRooms, AtomicInteger::get)
                .description("전체 채팅방 수").register(registry);
        Gauge.builder("follow_edges_active", activeFollowEdges, AtomicInteger::get)
                .description("활성 팔로우 엣지 수 (양방향이면 2)").register(registry);
    }

    private static String normCountry(String countryName) {
        return LOWER_TO_CANON.get(countryName.trim().toLowerCase());
    }

    private static String normInterestType(String t) {
        if (t == null) return "other";
        String v = t.trim().toLowerCase();
        return switch (v) {
            case "follow", "friend", "chat_room" -> v;
            default -> "other";
        };
    }

    public void recordFollowCreated() {
        followCreateCount.increment();
        activeFollowEdges.incrementAndGet();
    }

    public void recordChatRoomCreated(String roomId) {
        chatRoomCreateCount.increment();
        totalChatRooms.incrementAndGet();
        roomActiveMap.put(roomId, true);
        activeChatRooms.incrementAndGet();
    }

    public void recordChatRoomClosed(String roomId) {
        if (roomActiveMap.remove(roomId) != null) {
            activeChatRooms.decrementAndGet();
        }
    }

    public void recordChatMessage(byte[] payload) {
        chatMessageCount.increment();
        if (payload != null) chatMessageBytes.record(payload.length);
    }

    public void recordFollowCreated(String srcCountry, String dstCountry) {
        recordFollowCreated();
        registry.counter("feature_follow_edge_total",
                Tags.of("src_country", normCountry(srcCountry),
                        "dst_country", normCountry(dstCountry))
        ).increment();
        incInterestEdge(srcCountry, dstCountry, "follow");
    }

    public void recordFriendCreated(String srcCountry, String dstCountry) {
        registry.counter("feature_friend_edge_total",
                Tags.of("src_country", normCountry(srcCountry),
                        "dst_country", normCountry(dstCountry))
        ).increment();
        incInterestEdge(srcCountry, dstCountry, "friend");
    }

    private void incInterestEdge(String srcCountry, String dstCountry, String interestType) {
        registry.counter("feature_interest_edge_total",
                Tags.of("src_country", normCountry(srcCountry),
                        "dst_country", normCountry(dstCountry),
                        "interest_type", normInterestType(interestType))
        ).increment();
    }

    /**
     * 1:1 채팅 관심(방 생성) 기록용 신규 메서드
     */
    public void recordInterest(String srcCountry, String dstCountry, String interestType) {
        incInterestEdge(srcCountry, dstCountry, interestType); // "chat_room" 등
    }
}
