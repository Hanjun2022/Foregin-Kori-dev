package core.global.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 응답(JWT)")
public record AuthResponse(
        String tokenType,         // "Bearer"
        String accessToken,
        String refreshToken,
        long expiresInMillis,   // 액세스 토큰 만료(ms)
        Long userId,
        String email,
        Boolean isNewUser
) {
}
