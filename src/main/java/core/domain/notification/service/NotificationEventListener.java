package core.domain.notification.service;

import core.domain.notification.dto.NewUserJoinedEvent;
import core.domain.notification.dto.NotificationEvent;
import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // ✅ @Transactional 임포트

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final UserRepository userRepository;
    private final UserNotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final NotificationMessageGenerator notificationMessageGenerator;


    @Async("dispatchExecutor")
    @EventListener
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        try {

            User recipient = userRepository.findById(event.recipientId())
                    .orElse(null);
            User actor = userRepository.findById(event.actorId())
                    .orElse(null);

            if (recipient == null || actor == null) {
                log.warn("알림 이벤트 처리 실패: 수신자 또는 발신자를 찾을 수 없습니다. Event: {}", event);
                return;
            }

            log.info("알림 이벤트 수신: recipientId={}, actorId={}", recipient.getId(), actor.getId());

            String message = notificationMessageGenerator.generateMessage(actor, event);
            notificationService.createAndSaveNotification(recipient, actor, event, message);
            pushNotificationService.sendPushNotification(recipient, event, message);

        } catch (Exception e) {
            log.error("알림 이벤트 처리 중 오류 발생: {}", event, e);
        }
    }

    /**
     * ✅ [비효율적인 버전]
     * 신규 유저 가입 이벤트를 받아 전체 사용자에게 알림을 발송합니다.
     * 추후 최적화 필요
     */
    @Async("dispatchExecutor")
    @EventListener
    @Transactional
    public void handleNewUserBroadcastInefficiently(NewUserJoinedEvent event) {
        log.info("[비효율적 방식] 신규 유저 가입 이벤트 수신. 전체 알림 발송을 시작합니다. 신규 유저 ID: {}", event.newUserId());

        User newUserActor = userRepository.findById(event.newUserId()).orElse(null);
        if (newUserActor == null) {
            log.warn("신규 유저 정보를 찾을 수 없어 전체 알림을 중단합니다. ID: {}", event.newUserId());
            return;
        }

        NotificationEvent tempEvent = new NotificationEvent(
                null, newUserActor.getId(),
                core.global.enums.NotificationType.newuser,
                newUserActor.getId(), null, null);
        String message = notificationMessageGenerator.generateMessage(newUserActor, tempEvent);

        List<User> allUsers = userRepository.findAll();
        log.warn("[성능 경고] {}명의 모든 사용자를 메모리에 로드했습니다. 사용자 수가 많을 경우 OutOfMemoryError가 발생할 수 있습니다.", allUsers.size());

        for (User recipient : allUsers) {
            if (recipient.getId().equals(newUserActor.getId())) {
                continue;
            }

            try {
                notificationService.createAndSaveNotification(recipient, newUserActor, tempEvent, message);
                pushNotificationService.sendPushNotification(recipient, tempEvent, message);
            } catch (Exception e) {
                log.error("사용자 ID {}에게 신규 유저 알림 발송 중 오류 발생", recipient.getId(), e);
            }
        }
        log.info("[비효율적 방식] 전체 알림 발송 루프 완료. 총 {}명 처리.", allUsers.size());
    }
}