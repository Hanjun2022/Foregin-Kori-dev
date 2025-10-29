package core.domain.post.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 매우 단순한 prefix 사전:
 * - 키: 텍스트(제목/검색어/태그 등 짧은 후보)
 * - 값: 인기(pop) 점수
 * - prefix 조회는 정렬맵의 tailMap으로 근사
 */
@Component
public class SuggestMemoryIndex {

    // 사전: 사전순 정렬을 위해 SkipListMap 사용 (스레드-세이프 변형)
    private final ConcurrentSkipListMap<String, Integer> dict = new ConcurrentSkipListMap<>();

    /** prefix 기반 빠른 후보(정렬: pop desc → 길이 asc → 사전식) */
    public List<String> suggestPrefix(String prefix, int limit) {
        if (prefix == null || prefix.isBlank() || limit <= 0) return List.of();

        var it = dict.tailMap(prefix, true).entrySet().iterator();
        List<Map.Entry<String, Integer>> buf = new ArrayList<>(limit * 4);

        while (it.hasNext() && buf.size() < limit * 8) {
            var e = it.next();
            String key = e.getKey();
            if (!key.startsWith(prefix)) break;
            buf.add(e);
        }

        buf.sort((a, b) -> {
            int c = Integer.compare(b.getValue(), a.getValue()); // pop desc
            if (c != 0) return c;
            int c2 = Integer.compare(a.getKey().length(), b.getKey().length());
            if (c2 != 0) return c2;
            return a.getKey().compareTo(b.getKey());
        });

        return buf.stream().limit(limit).map(Map.Entry::getKey).toList();
    }

    /** 배치/실시간으로 후보 적재/점수 업데이트 */
    public void upsert(String text, int deltaPop) {
        String k = norm(text);
        if (k == null || k.isBlank()) return;
        dict.merge(k, deltaPop, Integer::sum);
    }

    private static String norm(String s) {
        if (s == null) return null;
        // 공백 트림 + 소문자 + NFC 정규화
        return Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }

}
