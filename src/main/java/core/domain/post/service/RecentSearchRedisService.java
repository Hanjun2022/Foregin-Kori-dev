package core.domain.post.service;

import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecentSearchRedisService {

    private static final int MAX = 10;                    // ✅ 사용자당 최대 보관 개수 8개
    private static final Duration TTL = Duration.ofDays(180);
    private final StringRedisTemplate redis;
    private final UserRepository userRepository;

    private static String keyOf(Long userId) {
        return "recent:" + userId;
    }

    public void log(Long userId, String raw) {
        if (userId == null || raw == null) return;
        String q = raw.trim();
        if (q.isEmpty()) return;
        q = q.toLowerCase(Locale.ROOT);

        String key = keyOf(userId);
        double now = System.currentTimeMillis();

        // upsert
        redis.opsForZSet().add(key, q, now);

        // 트림 (오래된 항목 제거)
        Long size = redis.opsForZSet().zCard(key);
        if (size != null && size > MAX) {
            long removeCount = size - MAX;              // 제거해야 할 개수
            if (removeCount > 0) {
                redis.opsForZSet().removeRange(key, 0, removeCount - 1);
            }
        }

        // TTL 연장
        redis.expire(key, TTL);
    }

    public List<String> list() {
        String key = keyOf(getUserId());

        int end = MAX - 1;

        Set<String> s = redis.opsForZSet().reverseRange(key, 0, end); // 최신순
        if (s == null || s.isEmpty()) return List.of();

        // 순서 유지용 복사
        return new ArrayList<>(s);
    }

    public void remove(String q) {
        redis.opsForZSet().remove(keyOf(getUserId()), q.toLowerCase(Locale.ROOT));
    }

    public void clear() {
        redis.delete(keyOf(getUserId()));
    }

    private Long getUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }
}