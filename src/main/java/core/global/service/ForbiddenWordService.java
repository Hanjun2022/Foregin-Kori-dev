package core.global.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ForbiddenWordService {

    // 단어 토큰화 패턴(한글/영문/숫자 포함)
    private static final Pattern WORD = Pattern.compile("\\p{L}[\\p{L}\\p{Nd}_'-]*");

    @Value("classpath:forbidden_words.json")
    private Resource forbiddenWordsFile;

    private List<String> forbiddenWords;

    // 소문자 정규화된 금칙어 Set (빠른 조회)
    private Set<String> forbiddenSet = Collections.emptySet();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> words = objectMapper.readValue(forbiddenWordsFile.getInputStream(), new TypeReference<List<String>>() {
            });

            this.forbiddenWords = words;

            Set<String> lowered = new HashSet<>();
            for (String w : words) {
                if (w != null) {
                    String t = w.trim();
                    if (!t.isEmpty()) {
                        lowered.add(t.toLowerCase(Locale.ROOT));
                    }
                }
            }
            this.forbiddenSet = lowered;

            log.info("금칙어 " + this.forbiddenWords.size() + "개를 로드했습니다.");

        } catch (IOException e) {
            log.warn("금칙어 파일을 로드하는 데 실패했습니다: " + e.getMessage());
            this.forbiddenWords = Collections.emptyList();
        }
    }

    public boolean containsForbiddenWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        Matcher m = WORD.matcher(text);

        int idx = 0;
        boolean found = false;

        while (m.find()) {
            String token = m.group();
            int start = m.start();
            int end = m.end();

            String normalized = token.toLowerCase(Locale.ROOT);
            if (forbiddenSet.contains(normalized)) {
                found = true;
            } else {
                log.trace("ok[{}]: '{}' (pos: {}-{})", idx, token, start, end);
            }
            idx++;
        }
        return found;
    }
}