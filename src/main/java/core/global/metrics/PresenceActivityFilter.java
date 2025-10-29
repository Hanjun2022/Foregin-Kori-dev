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

@Component
@RequiredArgsConstructor
public class PresenceActivityFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private static final long TTL_SECONDS = 300;
    private static final String ZKEY = "presence:active:zset"; // score = 만료 타임스탬프(ms)

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            String uid = auth.getName();
            long now = System.currentTimeMillis();
            long expireAt = now + TTL_SECONDS * 1000;

            redis.opsForZSet().add(ZKEY, uid, expireAt);
        }
        chain.doFilter(req, res);
    }

}

