package core.domain.chat.dto;

import core.domain.chat.entity.ChatRoom;

import java.time.Instant;
import java.util.List;

public record ChatRoomResponse(
        Long id,
        Boolean isGroup,
        Instant createdAt,
        List<ChatParticipantResponse> participants
) {
    public static ChatRoomResponse from(ChatRoom room) {
        List<ChatParticipantResponse> participantResponses = room.getParticipants().stream()
                .map(ChatParticipantResponse::from)
                .toList();

        return new ChatRoomResponse(
                room.getId(),
                room.getGroup(),
                room.getCreatedAt(),
                participantResponses
        );
    };

}
