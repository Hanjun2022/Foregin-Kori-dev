package core.global.dto;

public record LoginResponseDto(
        Long userId,
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}