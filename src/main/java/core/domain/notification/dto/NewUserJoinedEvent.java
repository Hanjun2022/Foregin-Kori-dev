package core.domain.notification.dto;


/**
 * 신규 유저 가입 사실을 알리는 이벤트 DTO
 * @param newUserId 새로 가입한 유저의 ID
 */
public record NewUserJoinedEvent(
        Long newUserId
) {
}