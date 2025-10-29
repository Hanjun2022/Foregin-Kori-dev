package core.domain.usernotificationsetting.dto;

import core.global.enums.NotificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


public record NotificationSettingBulkUpdateRequestDto(
        List<SettingItem> settings
) {
    public record SettingItem(
            NotificationType notificationType,
            boolean enabled
    ) {}
}
