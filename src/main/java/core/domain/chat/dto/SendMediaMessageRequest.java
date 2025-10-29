package core.domain.chat.dto;

import core.global.enums.MessageType;

public record SendMediaMessageRequest(
        Long roomId,
        Long senderId,
        MessageType messageType,
        String mediaKey
) {}