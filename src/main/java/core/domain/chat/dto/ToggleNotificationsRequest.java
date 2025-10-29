package core.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "채팅방 알림 설정 변경 요청 DTO")
public record ToggleNotificationsRequest(
        @NotNull(message = "enabled 필드는 필수입니다.")
        @Schema(description = "알림 활성화 여부", example = "true")
        Boolean enabled
) {
}