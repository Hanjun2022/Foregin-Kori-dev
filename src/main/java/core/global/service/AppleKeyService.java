package core.global.service;

import core.global.service.AppleClient;
import core.global.dto.ApplePublicKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleKeyService {

    private final AppleClient appleClient;

    /**
     * Apple의 공개키를 가져오는 메소드
     * @Cacheable: 이 메소드의 결과는 "applePublicKeys"라는 이름으로 캐시됩니다.
     * 동일한 요청이 오면 실제 메소드를 실행하지 않고 캐시된 값을 즉시 반환합니다.
     */
    @Cacheable(value = "applePublicKeys")
    public ApplePublicKeyResponse getApplePublicKeys() {
        // 이 로그는 캐시가 없을 때, 즉 최초 호출 시 또는 캐시 만료 시에만 출력됩니다.
        log.info("Fetching Apple Public Keys from Apple's Server...");
        return appleClient.getApplePublicKeys();
    }
}