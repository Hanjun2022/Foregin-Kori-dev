package core.domain.post.dto;

public record SearchResult(
        long id,
        String title,
        String snippet,
        double score
) {}
