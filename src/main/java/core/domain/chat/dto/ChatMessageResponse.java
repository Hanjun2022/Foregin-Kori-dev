package core.domain.chat.dto;

import core.global.enums.MessageType;

import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long senderId,
        String originContent,
        String targetContent,
        Instant sentAt,
        String senderFirstName,
        String senderLastName,
        String senderImageUrl,
        MessageType messageType) {

}