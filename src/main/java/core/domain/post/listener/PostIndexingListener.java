package core.domain.post.listener;

import core.domain.post.event.PostCreatedEvent;
import core.domain.post.event.PostUpdatedEvent;
import core.domain.post.service.SuggestMemoryIndex;
import core.global.service.SimpleKeywordExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostIndexingListener {

    private final SuggestMemoryIndex memoryIndex;
    private final SimpleKeywordExtractor keywordExtractor;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(PostCreatedEvent e) {
        if (e.content() != null) extractedKeyword(e.content(), 5);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdated(PostUpdatedEvent e) {
        if (e.content() != null) extractedKeyword(e.content(), 3);
    }

    private void extractedKeyword(String content, int score) {
        if (content != null && !content.isBlank()) {
            // 상위 K만 반영
            int K = 8;
            var phrases = keywordExtractor.extract(content, K);
            phrases.forEach(p -> memoryIndex.upsert(p, score));
        }
    }
}