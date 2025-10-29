package core.global.service;

import core.global.dto.ApplePublicKey;
import core.global.dto.ApplePublicKeyResponse;
import core.global.dto.AppleRefreshTokenResponse;
import core.global.dto.AppleTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "appleClient", url = "https://appleid.apple.com")
public interface AppleClient {

    /**
     * public key 요청
     */
    @GetMapping("/auth/keys")
    ApplePublicKeyResponse getApplePublicKeys();

    /**
     * 발급 받은 엑세스나 리프레쉬 토큰을 무효화 시킨다.
     * application/x-www-form-urlencoded 로 보내야 함
     */
    @PostMapping(value = "/auth/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void revoke(@RequestParam Map<String, ?> form);

    /**
     * 토큰 요청 (access+refresh+id 토큰 받음)
     * application/x-www-form-urlencoded 로 보내야 함
     */
    @PostMapping(value = "/auth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    AppleRefreshTokenResponse getToken(@RequestParam Map<String, ?> form);
}
