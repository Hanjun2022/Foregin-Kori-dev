package core.domain.chat.dto;

public record
TypingEvent(
        Long senderId,
        String senderName, // 보낸 사람의 이름
        Long roomId,
        boolean isTyping
) {}