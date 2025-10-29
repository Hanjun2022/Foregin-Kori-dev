package core.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationSettingStatusResponse(
        @Schema(description = "알림 설정 상태", example = "NEEDS_SETUP", allowableValues = {"NEEDS_SETUP", "CONFIGURED"})
        String status
) {}