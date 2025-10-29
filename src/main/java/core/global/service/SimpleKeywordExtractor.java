package core.global.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SimpleKeywordExtractor {

    private static final Pattern TOKEN = Pattern.compile("[\\p{IsHan}\\p{IsHangul}\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final Pattern NUMERIC_ONLY = Pattern.compile("^[0-9]+$");
    private static final Pattern REPEAT_RUN = Pattern.compile("(.)\\1{2,}"); // 같은 문자 3회 이상 연속 (ㅋㅋㅋ, aaa, !!!)

    public List<String> extract(String content, int maxCandidates) {
        if (content == null) return List.of();

        String norm = Normalizer.normalize(content, Normalizer.Form.NFC)
                .replaceAll("\\s+", " ")
                .trim();
        if (norm.isBlank()) return List.of();

        // 1) 토큰화 + 1차 휴리스틱 필터
        List<String> toks = new ArrayList<>();
        var m = TOKEN.matcher(norm);
        while (m.find()) {
            String t = m.group().toLowerCase(Locale.ROOT);
            if (t.length() < 2 || t.length() > 20) continue;
            if (NUMERIC_ONLY.matcher(t).matches()) continue;      // 숫자-only 배제
            if (REPEAT_RUN.matcher(t).find()) continue;           // 반복문자 배제
            if (!diverseEnough(t)) continue;                      // 문자 다양성(엔트로피 대용) 컷
            toks.add(t);
        }
        if (toks.isEmpty()) return List.of();

        // 2) unigram + bigram 스코어링
        Map<String, Score> scores = new HashMap<>();
        for (int i = 0; i < toks.size(); i++) {
            String uni = toks.get(i);
            bump(scores, uni, i, /*bigram*/ false);
            if (i + 1 < toks.size()) {
                String bi = uni + " " + toks.get(i + 1);
                if (validBigram(bi)) {
                    bump(scores, bi, i, /*bigram*/ true);
                }
            }
        }

        // 3) 정렬: 점수 desc → 길이 asc → 사전식
        List<Map.Entry<String, Score>> ranked = scores.entrySet().stream()
                .sorted((a, b) -> {
                    int c = Double.compare(b.getValue().score(), a.getValue().score());
                    if (c != 0) return c;
                    int c2 = Integer.compare(a.getKey().length(), b.getKey().length());
                    return (c2 != 0) ? c2 : a.getKey().compareTo(b.getKey());
                })
                .toList();

        // 4) 상위 N 반환
        return ranked.stream()
                .map(Map.Entry::getKey)
                .limit(maxCandidates)
                .collect(Collectors.toList());
    }

    // 문자 다양성(간이 엔트로피): 유니크 문자 비율이 너무 낮으면 제외
    private boolean diverseEnough(String t) {
        // 예: "ㅋㅋㅋㅋ" 같은 단조 패턴 컷
        int unique = (int) t.chars().distinct().count();
        double ratio = unique / (double) t.length();
        return ratio >= 0.3; // 0.3~0.35 사이로 튜닝 권장
    }

    private boolean validBigram(String bi) {
        // 양 끝 공백/중복 공백 방지, 숫자-only bigram 방지
        if (bi.isBlank()) return false;
        if (NUMERIC_ONLY.matcher(bi.replace(" ", "")).matches()) return false;
        return true;
    }

    private void bump(Map<String, Score> map, String key, int pos, boolean bigram) {
        Score s = map.getOrDefault(key, new Score());
        // TF 상한으로 과다 반복 방지 (문서 내 같은 토큰 5회까지만 기여)
        if (s.tf < 5) s.tf += 1;
        s.firstPos = Math.min(s.firstPos, pos);
        if (bigram) s.bigramBonus = 1.2; // bigram 우대(가중치 1.0 + 0.2)
        s.diversity = Math.max(s.diversity, diversity(key));
        map.put(key, s);
    }

    // 키 전체의 다양성(공백 포함), 점수 보정에 사용
    private double diversity(String key) {
        String k = key.replace(" ", "");
        int unique = (int) k.chars().distinct().count();
        return unique / (double) Math.max(1, k.length());
    }

    private static class Score {
        int tf = 0;
        int firstPos = Integer.MAX_VALUE;
        double bigramBonus = 1.0;
        double diversity = 0.0;

        double score() {
            // 위치 보너스: 앞부분 등장 가중(최대 1.5배)
            double posBonus = 1.0 + Math.max(0, 10 - Math.min(firstPos, 10)) * 0.05;
            // 다양성 보너스: 0~0.5 가산
            double diversityBonus = 1.0 + Math.min(0.5, diversity * 0.5);
            // 최종
            return tf * posBonus * bigramBonus * diversityBonus;
        }
    }
}
