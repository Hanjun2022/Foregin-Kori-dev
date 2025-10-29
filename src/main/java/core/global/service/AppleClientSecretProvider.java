package core.global.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AppleClientSecretProvider {
    /** Apple OAuth 설정(Team ID, Key ID, client_id, p8 PEM 등) */
    private final AppleOAuthProperties props;


    /**
     *
     * 이 부분이 Apple 서버로 보낼 client_secret (JWT) 을 만들 때 필요한 EC 개인키를 로드하는 부분이에요.
     * @return
     */
    private PrivateKey loadECPrivateKey() {
        try {
            String pem = props.privateKeyPem()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] pkcs8 = Base64.getDecoder().decode(pem);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        } catch (Exception e) {
            throw new IllegalStateException("Apple p8 로드 실패", e);
        }
    }

}
