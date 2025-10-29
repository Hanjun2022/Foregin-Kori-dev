package core.domain.usernotificationsetting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingInitRequestDto {
    private List<NotificationSettingInitItem> settings;
}
