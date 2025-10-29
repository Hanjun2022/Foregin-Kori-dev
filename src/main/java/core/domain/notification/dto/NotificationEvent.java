package core.domain.notification.dto;


import core.global.enums.NotificationType;

/**
 * 알림 이벤트 DTO
 *
 * @param recipientId      알림 수신자 ID
 * @param actorId          알림 유발자 ID
 * @param notificationType 알림 유형
 * @param referenceId      관련 콘텐츠의 주요 ID (예: postId, chatRoomId)
 * @param commentId        관련 댓글 ID (댓글/답글 알림 전용, 그 외에는 null)
 * @param contentSnippet   콘텐츠 일부 (예: 채팅 메시지 내용)
 */
public record NotificationEvent(
        Long recipientId,
        Long actorId,
        NotificationType notificationType,
        Long referenceId,
        Long commentId,
        String contentSnippet
) {
    public NotificationEvent(Long recipientId, Long actorId, NotificationType notificationType, Long referenceId, String contentSnippet) {
        this(recipientId, actorId, notificationType, referenceId, null, contentSnippet);
    }
}