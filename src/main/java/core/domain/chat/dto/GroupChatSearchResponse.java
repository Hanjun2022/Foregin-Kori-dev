package core.domain.chat.dto;

import core.domain.chat.entity.ChatRoom;

public record GroupChatSearchResponse(
        Long chatRoomId,
        String roomName,
        String description,
        String roomImageUrl,
        int participantCount
) {
    public static GroupChatSearchResponse from(
            ChatRoom chatRoom,
            String roomImageUrl,
            int participantCount) {
        return new GroupChatSearchResponse(
                chatRoom.getId(),
                chatRoom.getRoomName(),
                chatRoom.getDescription(),
                roomImageUrl,
                participantCount
        );
    }
}