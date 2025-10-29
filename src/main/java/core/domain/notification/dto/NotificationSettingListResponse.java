package core.domain.notification.dto;

import java.util.List;

public record NotificationSettingListResponse(
        List<NotificationSettingResponse> settings
) {}
