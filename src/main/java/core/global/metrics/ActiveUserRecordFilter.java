package core.global.metrics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ActiveUserRecordFilter extends OncePerRequestFilter {

    private final UserActivityRecorder userActivityRecorder;
    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            String uid = auth.getName(); // 내부 userId 문자열이면 더 좋음

            String throttleKey = "seen:active:" + uid;
            Boolean firstSeen = redis.opsForValue().setIfAbsent(throttleKey, "1", Duration.ofHours(1));
            if (Boolean.TRUE.equals(firstSeen)) {
                userActivityRecorder.recordActiveUser(uid); // ✅ HLL 적재
            }

            LocalDateTime now = LocalDateTime.now();
            String hourKey = "hll:active:hour:" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH"));
            String minKey  = "hll:active:min:"  + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm"));
            redis.opsForHyperLogLog().add(hourKey, uid);
            redis.opsForHyperLogLog().add(minKey, uid);
        }
        chain.doFilter(req, res);
    }
}