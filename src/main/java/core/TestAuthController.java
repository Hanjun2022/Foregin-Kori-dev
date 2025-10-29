package core;

// [임시] 테스트 로그인 컨트롤러

import core.global.config.JwtTokenProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class TestAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    // [임시] 테스트 로그인 엔드포인트
    @PostMapping("/test-login")
    public ResponseEntity<Map<String, Object>> testLogin(@RequestBody TestLoginRequest req) {
        // JwtTokenProvider의 기존 createToken 메서드 그대로 사용
        String token = jwtTokenProvider.createAccessToken(1L, req.getEmail());
        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "issuedAt", Instant.now().toString()
        ));
    }

    // [임시] 토큰 인증 테스트용 엔드포인트
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }
        return ResponseEntity.ok(Map.of(
                "name", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }

    @Data
    public static class TestLoginRequest {
        private String email; // [임시] 테스트용 이메일
    }
}
