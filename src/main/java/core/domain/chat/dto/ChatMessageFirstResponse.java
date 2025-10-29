package core.domain.chat.dto;

import core.domain.chat.entity.ChatMessage;
import core.domain.chat.entity.ChatRoom;
import core.domain.user.entity.User;
import core.global.enums.ImageType;
import core.global.image.entity.Image;
import core.global.image.repository.ImageRepository;

import java.time.Instant;

public record ChatMessageFirstResponse(
        Long id,
        Long roomId,
        Long senderId,
        String senderFirstName,
        String senderLastName,
        String senderImageUrl,
        String content,
        Instant sentAt
) {
    public static ChatMessageFirstResponse fromEntity(ChatMessage message, ChatRoom room, ImageRepository imageRepository) {
        User sender = message.getSender();
        String senderImageUrl;
        senderImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                .map(Image::getUrl)
                .orElse(null);


        return new ChatMessageFirstResponse(
                message.getId(),
                message.getChatRoom().getId(),
                sender.getId(),
                sender.getFirstName(),
                sender.getLastName(),
                senderImageUrl,
                message.getContent(),
                message.getSentAt()
        );
    }
}