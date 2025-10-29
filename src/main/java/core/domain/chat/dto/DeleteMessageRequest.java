package core.domain.chat.dto;

public record DeleteMessageRequest(
        Long messageId,
        Long senderId
) {}