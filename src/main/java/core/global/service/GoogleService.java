package core.global.service;

import core.global.dto.AccessTokenDto;
import core.global.dto.GoogleProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 구글 OAuth 2.0 인증 및 사용자 정보 조회를 위한 통합 서비스 클래스.
 * 웹 애플리케이션용 일반 인증 코드 흐름과 모바일 클라이언트용 PKCE 흐름을 모두 지원합니다.
 * RestClient를 사용하여 HTTP 통신을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class GoogleService {

    @Value("${oauth.google.web.client-id}")
    private String webClientId;
    @Value("${oauth.google.web.client-secret}")
    private String webClientSecret;
    @Value("${oauth.google.web.redirect-uri}")
    private String webRedirectUri;


    @Value("${oauth.google.android.client-id}")
    private String androidClientId;
    @Value("${oauth.google.android.redirect-uri}")
    private String androidRedirectUri;

    @Value("${oauth.google.ios.client-id}")
    private String iosClientId;
    @Value("${oauth.google.ios.redirect-uri}")
    private String iosRedirectUri;

    private final RestClient restClient = RestClient.create();


    public AccessTokenDto exchangeCode(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", webClientId);
        formData.add("client_secret", webClientSecret);
        formData.add("redirect_uri", webRedirectUri);

        ResponseEntity<AccessTokenDto> response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toEntity(AccessTokenDto.class);

        return response.getBody();
    }

    /**
     * Access Token을 사용하여 구글 프로필 조회
     */
    public GoogleProfileDto getGoogleProfile(String accessToken) {
        ResponseEntity<GoogleProfileDto> response = restClient.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toEntity(GoogleProfileDto.class);
        return response.getBody();
    }

}

