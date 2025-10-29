package core.domain.chat.dto;

import core.domain.chat.entity.ChatParticipant;
import core.global.enums.ChatParticipantStatus;

import java.time.Instant;

public record ChatParticipantResponse(
        Long id,
        Long userId,
        String userName,
        Instant joinedAt,
        Instant lastLeftAt,
        Long lastReadMessageId,
        ChatParticipantStatus status
) {
    public static ChatParticipantResponse from(ChatParticipant participant) {
        return new ChatParticipantResponse(
                participant.getId(),
                participant.getUser().getId(),
                participant.getUser().getLastName(),
                participant.getJoinedAt(),
                participant.getLastLeftAt(),
                participant.getLastReadMessageId(),
                participant.getStatus()
        );
    }
}