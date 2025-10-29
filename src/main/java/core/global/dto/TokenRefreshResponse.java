package core.global.dto;


public record TokenRefreshResponse(String accessToken, String refreshToken,Long userId) {
}