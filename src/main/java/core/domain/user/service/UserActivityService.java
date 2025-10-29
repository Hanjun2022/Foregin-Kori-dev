package core.domain.user.service;

import core.domain.user.repository.UserRepository;
import core.global.metrics.ReactivationCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserRepository userRepository;
    private final ReactivationCounter reactivationCounter;

    @Async
    @Transactional
    public void updateLastSeenAt(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            Instant prev = user.getLastSeenAt();
            boolean wasDormant = (prev == null) || prev.isBefore(Instant.now().minus(30, ChronoUnit.DAYS));

            if (user.getLastSeenAt() == null ||
                ChronoUnit.MINUTES.between(user.getLastSeenAt(), Instant.now()) > 5) {
                user.updateLastSeenAt(); // 기존 메서드 그대로
            }

            if (wasDormant) {
                reactivationCounter.inc(); // ← 30d+ 미접속 → 이번에 복귀 이벤트
            }
        });
    }
}
