package core.domain.post.event;

public record PostCreatedEvent(Long postId, String content) {}