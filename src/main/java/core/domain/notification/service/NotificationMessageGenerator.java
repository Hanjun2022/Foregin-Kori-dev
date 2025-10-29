package core.domain.notification.service;

import core.domain.notification.dto.NotificationEvent;
import core.domain.user.entity.User;
import core.global.enums.NotificationType; // NotificationType enum 임포트
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageGenerator {

    /**
     * NotificationEvent를 기반으로 사용자에게 표시될 메시지를 생성합니다.
     * @param event 알림 이벤트 데이터
     * @return 생성된 메시지 문자열
     */
    public String generateMessage(User actor , NotificationEvent event) {
        String actorName = actor.getFirstName();
        if (actorName == null || actorName.isBlank()) {
            actorName = "누군가";
        }

        return switch (event.notificationType()) {

            case chat -> {
                String snippet = event.contentSnippet();
                if (snippet != null && snippet.length() > 30) {
                    snippet = snippet.substring(0, 30) + "...";
                }
                yield actorName + "님으로부터 새로운 메시지: " + snippet;
            }

            case post -> actorName + " commented on your post.";
            case comment -> actorName + " replied to your comment.";
            case follow -> actorName + " accepted your follow request.";
            case receive -> actorName + " started following you.";
            case newuser -> actorName + " just joined! Say hello!";
            case followuserpost -> actorName + " posted something new.";
            default -> "새로운 알림이 도착했습니다.";
        };
    }
}