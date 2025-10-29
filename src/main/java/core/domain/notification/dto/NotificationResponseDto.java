package core.domain.notification.dto;

import core.global.enums.NotificationType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationResponseDto(
        Long notificationId,
        NotificationType notificationType,
        String message,
        boolean isRead,
        Instant createdAt,
        ActorDto actor,
        Long referenceId,
        Long subReferenceId
) {}