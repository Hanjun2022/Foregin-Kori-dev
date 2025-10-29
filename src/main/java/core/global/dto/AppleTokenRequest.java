package core.global.dto;


import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class AppleTokenRequest {
    private String grantType;      // 항상 "authorization_code"
    private String code;
    private String redirectUri;
    private String clientId;
    private String clientSecret;
    private String codeVerifier;
}