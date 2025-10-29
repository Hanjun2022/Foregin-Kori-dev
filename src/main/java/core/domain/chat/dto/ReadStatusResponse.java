package core.domain.chat.dto;


public record ReadStatusResponse(
        Long roomId,
        Long readerId,
        Long lastReadMessageId
) {}