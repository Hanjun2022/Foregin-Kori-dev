package core.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import core.domain.chat.entity.ChatParticipant;
import core.domain.chat.repository.ChatParticipantRepository;
import core.domain.notification.dto.NotificationEvent;
import core.domain.user.entity.User;
import core.domain.userdevicetoken.entity.UserDeviceToken;
import core.domain.userdevicetoken.repository.UserDeviceTokenRepository;
import core.domain.usernotificationsetting.entity.UserNotificationSetting;
import core.domain.usernotificationsetting.repository.UserNotificationSettingRepository;
import core.global.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    /**
     * 사용자에게 푸시 알림을 발송합니다. (람다 제거 버전)
     * 발송 전 3단계 동의 여부를 모두 확인합니다.
     * 클라이언트 이동을 위한 data 페이로드를 포함합니다.
     * @param event 알림 이벤트 데이터
     * @param message 사용자에게 보여줄 최종 메시지
     */
    public void sendPushNotification(User recipient, NotificationEvent event, String message) {

        if (!recipient.isAgreedToPushNotification()) {
            log.info("사용자 ID {}: 마스터 스위치 OFF. 푸시 알림을 발송하지 않습니다.", recipient.getId());
            return;
        }

        if (event.notificationType().isConfigurable()) {
            Optional<UserNotificationSetting> categorySettingOpt = userNotificationSettingRepository.findByUserAndNotificationType(recipient, event.notificationType());
            boolean isCategoryEnabled = categorySettingOpt.map(UserNotificationSetting::isEnabled).orElse(true);

            if (!isCategoryEnabled) {
                log.info("사용자 ID {}: '{}' 카테고리 스위치 OFF. 푸시 알림을 발송하지 않습니다.", recipient.getId(), event.notificationType());
                return;
            }
        }

        if (event.notificationType() == NotificationType.chat) {
            boolean isRoomNotificationsEnabled;
            Optional<ChatParticipant> participantOpt = chatParticipantRepository.findByChatRoomIdAndUserId(event.referenceId(), recipient.getId());
            isRoomNotificationsEnabled = participantOpt.map(ChatParticipant::isNotificationsEnabled).orElse(false);

            if (!isRoomNotificationsEnabled) {
                log.info("사용자 ID {}: 채팅방 ID {} 음소거 상태. 푸시 알림을 발송하지 않습니다.", recipient.getId(), event.referenceId());
                return;
            }
        }

        List<UserDeviceToken> deviceTokens = userDeviceTokenRepository.findAllByUser(recipient);
        for (UserDeviceToken userDeviceToken : deviceTokens) {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(userDeviceToken.getDeviceToken())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle("Foreigner")
                            .setBody(message)
                            .build())
                    .putData("notificationType", event.notificationType().name());

            switch (event.notificationType()) {
                case post:
                    messageBuilder
                            .putData("type", "post")
                            .putData("postId", String.valueOf(event.referenceId()));
                    if (event.commentId() != null) {
                        messageBuilder.putData("commentId", String.valueOf(event.commentId()));
                    }
                    break;
                case  comment:
                    messageBuilder
                            .putData("type", "comment")
                            .putData("postId", String.valueOf(event.referenceId()));
                    if (event.commentId() != null) {
                        messageBuilder.putData("commentId", String.valueOf(event.commentId()));
                    }
                case follow:
                    messageBuilder
                            .putData("type", "follow")
                            .putData("friendId", String.valueOf(event.actorId()));
                    break;
                case receive:
                    messageBuilder
                            .putData("type", "receive")
                            .putData("followerId", String.valueOf(event.actorId()));
                    break;
                case chat:
                    messageBuilder
                            .putData("type", "chat")
                            .putData("roomId", String.valueOf(event.referenceId()))
                            .putData("myId", String.valueOf(recipient.getId()));
                    break;
                case newuser:
                    messageBuilder
                            .putData("type", "newuser")
                            .putData("userId", String.valueOf(event.referenceId()));
                    break;
                case followuserpost:
                    messageBuilder
                            .putData("type", "followuserpost")
                            .putData("postId", String.valueOf(event.referenceId()));
                    break;
                default:
                    break;
            }

            Message fcmMessage = messageBuilder.build();

            try {
                firebaseMessaging.send(fcmMessage);
                log.info("사용자 ID {} 에게 푸시 알림을 성공적으로 발송했습니다. (기기 토큰: ...{})", recipient.getId(), userDeviceToken.getDeviceToken().substring(userDeviceToken.getDeviceToken().length() - 5));
            } catch (FirebaseMessagingException e) {
                log.error("푸시 알림 발송 실패: 사용자 ID {}", recipient.getId(), e);
                // TODO: 만료된 토큰 등 FCM 예외에 대한 후처리 로직 (예: DB에서 토큰 삭제)
            }
        }
    }
}