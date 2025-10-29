package core.domain.post.repository;

import core.domain.post.dto.SearchResultView;

import java.time.Instant;
import java.util.List;

public interface PostSearchRepositoryCustom {
    List<String> suggest(String q, Long resolvedBoardId, List<Long> blockedIds, int limit);
    List<SearchResultView> search(String q,Long userId,  Long resolvedBoardId, List<Long> blockedIds, Instant afterTime, Long afterId, int i);
    List<String> findHotKeywordsOrTitles(int topN);
}

