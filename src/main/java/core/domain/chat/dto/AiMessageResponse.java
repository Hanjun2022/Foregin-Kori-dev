package core.domain.chat.dto;


import core.domain.chat.entity.ChatMessage;
import lombok.Builder;

import java.time.Instant;

@Builder
public record AiMessageResponse(
        Long messageId,
        Long senderId,
        String senderName, // 보낸 사람 이름 추가
        String content,
        Instant sentAt
) {
    public static AiMessageResponse from(ChatMessage message) {
        return AiMessageResponse.builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName()+" "+message.getSender().getLastName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }
}