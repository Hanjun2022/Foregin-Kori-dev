package core.global.service;

import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import core.global.dto.ResetToken;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final SmtpMailService smtpService;
    private final PasswordEncoder passwordEncoder;
    private static final String EMAIL_VERIFY_CODE_KEY = "email_verification:code:";     // code 보관
    private static final long CODE_TTL_MIN = 3L; // 분

    /**
     * 이메일 보내주는 로직
     */
    public void sendEmailVerificationCode(String rawEmail, Locale locale) {
        String email = normalizeEmail(rawEmail);
        Duration ttl = Duration.ofMinutes(CODE_TTL_MIN);

        String verificationCode = smtpService.sendVerificationEmail(
                email,
                ttl,
                locale
        );

        redisTemplate.opsForValue().set(
                EMAIL_VERIFY_CODE_KEY + email,
                verificationCode,
                CODE_TTL_MIN,
                TimeUnit.MINUTES
        );

        log.info("비밀번호 재설정 코드 발송 완료: {}", email);
    }

    @Transactional
    public void verifyCodeAndResetPassword(String rawEmail, String newPassword) {
        String email = normalizeEmail(rawEmail);
        log.info("[비밀번호 재설정] 요청 시작 - email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[비밀번호 재설정] 사용자 없음 - email: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        // Note: The code verification logic has been removed as per the request.
        // The password will now be changed directly if a user with the provided email is found.

        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("[비밀번호 재설정] 비밀번호 변경 완료 - email: {}", email);
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
