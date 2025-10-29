package core.domain.chat.dto;

import core.domain.chat.entity.ChatRoom;

import java.util.List;

public record GroupChatDetailResponse(
        Long chatRoomId,
        String roomName,
        String description,
        Long ownerId, // owner_id -> ownerId
        String ownerFirstName,
        String ownerLastName,
        String roomImageUrl,
        int participantCount,
        List<String> participantsImageUrls,
        String ownerImageUrl
) {
    public static GroupChatDetailResponse from(
            ChatRoom chatRoom,
            String roomImageUrl,
            int participantCount,
            List<String> participantsImageUrls,
            String ownerImageUrl
    ) {
        return new GroupChatDetailResponse(
                chatRoom.getId(),
                chatRoom.getRoomName(),
                chatRoom.getDescription(),
                chatRoom.getOwner().getId(),
                chatRoom.getOwner().getFirstName(),
                chatRoom.getOwner().getLastName(),
                roomImageUrl,
                participantCount,
                participantsImageUrls,
                ownerImageUrl // 생성자에 ownerImageUrl 전달
        );
    }
}