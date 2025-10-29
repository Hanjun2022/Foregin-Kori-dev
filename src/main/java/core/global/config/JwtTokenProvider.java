package core.global.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final Key SECRET_KEY;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    private final String keyHash;

    private static String shortHash(byte[] key) {
        int h = 1;
        for (byte b : key) h = 31 * h + (b & 0xff);
        return String.format("%08x", h); // 항상 8자리로 패딩
    }

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKeyBase64,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration
    ) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKeyBase64.trim()); // Base64로 보관된 경우
        } catch (IllegalArgumentException e) {
            keyBytes = secretKeyBase64.getBytes(StandardCharsets.UTF_8); // 평문으로 보관된 경우
        }
        this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration * 60 * 1000L;
        this.refreshTokenExpiration = refreshTokenExpiration * 60 * 1000L;

        this.keyHash = shortHash(keyBytes);
        org.slf4j.LoggerFactory.getLogger(JwtTokenProvider.class)
                .info("[JWT] Provider init: alg=HS512, keyLen={}B, keyHash={}", keyBytes.length, this.keyHash);
    }

    // 진단용 getter
    public String keyHash() { return keyHash; }

    /**
     * 액세스 토큰을 생성합니다.
     * userId와 email을 Claims에 포함시킵니다.
     */
    public String createAccessToken(Long userId, String email) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("id", userId);
        claims.setId(UUID.randomUUID().toString());
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 리프레시 토큰을 생성합니다.
     * userId를 Claims와 Subject에 포함시킵니다.
     */
    public String createRefreshToken(Long userId) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.setId(UUID.randomUUID().toString());
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }
    /**
     * 토큰에서 이메일(subject)을 추출합니다.
     */
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Map<String, String> parseHeaders(String token) throws JsonProcessingException {
        String header = token.split("\\.")[0];
        return new ObjectMapper().readValue(decodeHeader(header), Map.class);
    }
    private String decodeHeader(String token) {
        return new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
    }
    public Claims getTokenClaims(String token, PublicKey publicKey) {
        try {
            return Jwts.parser()
                    .setSigningKey(publicKey)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException | MalformedJwtException e) {
            throw new BusinessException(ErrorCode.INVALID_JWT);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.JWT_EXPIRED);
        }
    }
    /**
     * 액세스 토큰에서 사용자 ID를 추출합니다.
     */
    public Long getUserIdFromAccessToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("id", Long.class);
    }

    /**
     * 토큰의 유효성을 검증합니다.
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
        return true;
    }

    /**
     * Redis 엑세스 토큰이 만약 만료가 안 되면
     *
     */
    /**
     * 토큰의 만료 시간을 추출합니다.
     */
    public Date getExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }
    /**
     * 리프레시 토큰에서 사용자 ID를 추출합니다.
     */

    public Long getUserIdFromRefreshToken(String token) {
        return Long.valueOf(
                Jwts.parserBuilder()
                        .setSigningKey(SECRET_KEY)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }



}