package core.domain.usernotificationsetting.dto;

import core.global.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationSettingResponseDto {
    private NotificationType notificationType;
    private boolean enabled;
}
