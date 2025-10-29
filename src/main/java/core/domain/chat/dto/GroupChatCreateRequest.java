package core.domain.chat.dto;

public record GroupChatCreateRequest(
        String roomName,
        String description,
        String roomImageUrl
) {}