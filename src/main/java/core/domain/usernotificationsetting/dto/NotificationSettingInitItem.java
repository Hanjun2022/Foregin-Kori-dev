package core.domain.usernotificationsetting.dto;

import core.global.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingInitItem {
    private NotificationType notificationType;
    private boolean enabled;
}