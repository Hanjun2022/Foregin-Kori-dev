package core.global.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 채팅 서비스가 메인 서비스에 채팅방 이미지 생성을 요청할 때 사용하는 DTO입니다.
 * (내부 서비스 간 통신용)
 *
 * @param chatRoomId 이미지를 설정할 채팅방의 ID
 * @param imageUrl   저장할 이미지의 URL
 */
public record UpsertChatRoomImageRequest(

        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long chatRoomId,

        @NotBlank(message = "이미지 URL은 필수입니다.")
        String imageUrl
) {
}