package core.domain.chat.dto;

import core.domain.chat.entity.ChatParticipant;
import core.domain.chat.entity.ChatRoom;
import core.domain.user.entity.User;
import core.global.enums.ImageType;
import core.global.image.entity.Image;
import core.global.image.repository.ImageRepository;

import java.time.Instant;

public record ChatRoomSummaryResponse(
        Long roomId,
        String roomName,
        String lastMessageContent,
        Instant lastMessageTime,
        String roomImageUrl,
        int unreadCount,
        int participantCount
) {}