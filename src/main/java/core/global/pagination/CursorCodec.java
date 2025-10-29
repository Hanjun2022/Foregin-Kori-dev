package core.global.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** 정렬유형별로 필요한 키만 넣어 encode 하세요.
 *  예) 최신: t(Instant), id(Long)
 *      인기: sc(score, Long), id(Long)
 *      단순 ID: id(Long)
 *  필요 시 키추가 가능(예: lc=likeCount)
 */
public final class CursorCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CursorCodec() {}

    public static String encode(Map<String, Object> payload) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return Map.of();
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8));
            return MAPPER.readValue(json, HashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }

    public static String encodeLatest(Instant t, Long id) {
        Map<String, Object> m = new HashMap<>();
        m.put("t", t == null ? null : t.toString());
        m.put("id", id);
        return encode(m);
    }

    public static String encodePopular(Long score, Long id) {
        Map<String, Object> m = new HashMap<>();
        m.put("sc", score);
        m.put("id", id);
        return encode(m);
    }

    public static String encodeId(Long id) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        return encode(m);
    }
}