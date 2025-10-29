package core.global.service;

import core.global.dto.ApplePublicKey;
import core.global.dto.ApplePublicKeyResponse;
import core.global.exception.BusinessException;
import core.global.enums.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
public class ApplePublicKeyGenerator {

    /**
     * 토큰 헤더와 공개키 목록을 받아, 일치하는 공개키(PublicKey) 객체를 생성하여 반환합니다.
     * 이 클래스의 유일한 public 메소드입니다.
     */
    public PublicKey generate(Map<String, String> tokenHeaders, ApplePublicKeyResponse publicKeyResponse) {
        // 1. DTO의 로직을 이곳으로 가져와서, 헤더 정보와 일치하는 키를 찾습니다.
        ApplePublicKey matchedKey = publicKeyResponse.keys().stream()
                .filter(key -> key.kid().equals(tokenHeaders.get("kid")) && key.alg().equals(tokenHeaders.get("alg")))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_MATCHING_APPLE_KEY)); // 또는 적절한 예외

        // 2. 찾은 키를 사용하여 PublicKey 객체를 생성합니다.
        return createPublicKey(matchedKey);
    }

    /**
     * ApplePublicKey DTO를 실제 PublicKey 객체로 변환하는 private 헬퍼 메소드입니다.
     */
    private PublicKey createPublicKey(ApplePublicKey keyDto) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(keyDto.n());
            byte[] eBytes = Base64.getUrlDecoder().decode(keyDto.e());

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
            KeyFactory keyFactory = KeyFactory.getInstance(keyDto.kty());

            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new BusinessException(ErrorCode.PUBLIC_KEY_GENERATION_FAILED);
        }
    }
}
