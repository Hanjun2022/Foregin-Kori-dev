package core.domain.chat.dto;

public record SendMessageRequest(
        Long roomId,
        Long senderId,
        String content
) {
}