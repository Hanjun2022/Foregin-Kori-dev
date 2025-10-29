package core.domain.notification.dto;

import core.global.enums.NotificationType;

public record NotificationSettingResponse(
        NotificationType type,
        boolean enabled
) {}