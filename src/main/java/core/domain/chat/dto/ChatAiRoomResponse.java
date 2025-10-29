package core.domain.chat.dto;


import core.domain.chat.entity.ChatRoom;
import lombok.Builder;

@Builder
public record ChatAiRoomResponse(
        Long roomId,
        String roomName,
        boolean isNew
) {
    /**
     * ChatRoom 엔티티와 생성 여부(isNew)를 받아 DTO를 생성합니다.
     */
    public static ChatAiRoomResponse of(ChatRoom chatRoom, boolean isNew) {
        return ChatAiRoomResponse.builder()
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getRoomName())
                .isNew(isNew)
                .build();
    }
}