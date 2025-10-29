package core.global.service;

import core.domain.user.entity.User;
import core.domain.user.service.UserService;
import core.global.config.JwtTokenProvider;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
@Slf4j
@RequiredArgsConstructor
@Service
public class AppleWithdrawalService {

    private final AppleOAuthProperties appleProps;
    private final AppleClient appleClient;
    private final AppleClientSecretGenerator clientSecretGenerator;

    /**
     * Apple 서버에 토큰 무효화를 요청하는 메인 메소드
     * @param user 탈퇴할 사용자 엔티티
     */
    public void revokeAppleToken(User user) {
        String appleRefreshToken = user.getAppleRefreshToken();

        if (appleRefreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_APPLE_REFRESH_TOKEN);
        }
        String clientSecret = clientSecretGenerator.generateRevokeClientSecret();
        MultiValueMap<String, String> formData = createRevokeFormData(clientSecret, appleRefreshToken);

        try {
            log.info("Sending token revocation request to Apple server for user ID: {}", user.getId());
            appleClient.revoke(formData);
            log.info("Successfully revoked Apple token for user ID: {}", user.getId());
        } catch (FeignException e) {
            log.error("Apple server returned an error during token revocation.");
            log.error("Status: {}, Reason: {}", e.status(), e.contentUTF8());
            throw new BusinessException(ErrorCode.INVALID_APPLE_REQUEST);
        } catch (Exception e) {
            log.error("An unexpected error occurred during Apple token revocation.", e);
            throw new BusinessException(ErrorCode.INVALID_APPLE_REQUEST);
        }
    }

    /**
     * Apple /auth/revoke 엔드포인트에 필요한 Form Data를 생성하는 private 메소드
     */
    private MultiValueMap<String, String> createRevokeFormData(String clientSecret, String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", appleProps.clientId());
        formData.add("client_secret", clientSecret);
        formData.add("token", refreshToken);
        formData.add("token_type_hint", "refresh_token");
        return formData;
    }
}
