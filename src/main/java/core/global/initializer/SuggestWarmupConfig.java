package core.global.initializer;

import core.domain.post.repository.PostSearchRepositoryCustom;
import core.domain.post.service.SuggestMemoryIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SuggestWarmupConfig {

    private final PostSearchRepositoryCustom searchRepository;
    private final SuggestMemoryIndex memoryIndex;

    @Bean
    ApplicationRunner suggestWarmupRunner() {
    log.info("runner start");
        return args -> {
            int topN = 2000;
            List<String> hotKeys = searchRepository.findHotKeywordsOrTitles(topN);
            System.out.println("[Warmup] hotKeys size = " + (hotKeys == null ? 0 : hotKeys.size()));
            hotKeys.stream().limit(10).forEach(k -> System.out.println("[Warmup] sample=" + k));

            for (String k : hotKeys) {
                memoryIndex.upsert(k, 1);
            }
            System.out.println("[Warmup] memoryIndex loaded.");
        };

    }
}
