package core.domain.chat.dto;


public record GroupChatMainResponse(
        Long roomId,
        String roomName,
        String description,
        String roomImageUrl,
        String userCount
) {
}