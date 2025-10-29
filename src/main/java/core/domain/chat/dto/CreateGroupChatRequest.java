package core.domain.chat.dto;


import jakarta.validation.constraints.NotBlank;

public record CreateGroupChatRequest(
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        String roomName,

        String description,
        String roomImageUrl
) {
}