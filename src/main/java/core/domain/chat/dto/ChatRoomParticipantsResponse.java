package core.domain.chat.dto;

public record ChatRoomParticipantsResponse(
        Long userId,
        String firstName,
        String lastName,
        String userImageUrl,
        boolean isHost
) {
}