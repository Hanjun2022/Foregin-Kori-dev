package core.global.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * 로그인 완료시 애플 서버에서 받아와야 하는 정보
 */
// Simplified and more practical response for the client
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AppleLoginResult {
    private String accessToken;
    private String refreshToken;
    private Long userId;        // Your service's user ID
    private String userEmail;     // User's email from Apple ID Token
    private boolean isNewUser;    // Flag for onboarding
}