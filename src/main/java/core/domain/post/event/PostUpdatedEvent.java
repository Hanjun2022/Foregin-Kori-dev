package core.domain.post.event;

public record PostUpdatedEvent(Long postId, String content) {}