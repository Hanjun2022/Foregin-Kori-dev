package core.domain.notification.service;

import com.google.cloud.PageImpl;
import core.domain.notification.dto.*;
import core.domain.notification.entity.Notification;
import core.domain.notification.repository.NotificationRepository;
import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import core.domain.userdevicetoken.entity.UserDeviceToken;
import core.domain.userdevicetoken.repository.UserDeviceTokenRepository;
import core.domain.usernotificationsetting.entity.UserNotificationSetting;
import core.domain.usernotificationsetting.repository.UserNotificationSettingRepository;
import core.global.enums.ErrorCode;
import core.global.enums.ImageType;
import core.global.enums.NotificationType;
import core.global.exception.BusinessException;
import core.global.image.dto.NotificationSliceResponseDto;
import core.global.image.entity.Image;
import core.global.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList; // ArrayList import 추가
import java.util.List;
import java.util.Map;
import java.util.Optional; // Optional import 추가
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserRepository userRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final NotificationRepository notificationRepository;
    private final ImageRepository imageRepository;
    /**
     * FCM 기기 토큰을 등록하거나 갱신합니다. (람다 제거 버전)
     */
    public void registerDeviceToken(Long userId, String deviceToken) {
        User user = findUserById(userId);

        Optional<UserDeviceToken> optionalToken = userDeviceTokenRepository.findByDeviceToken(deviceToken);

        if (optionalToken.isPresent()) {
            UserDeviceToken existingToken = optionalToken.get();
            existingToken.updateUser(user);
        } else {
            UserDeviceToken newToken = new UserDeviceToken(user, deviceToken);
            userDeviceTokenRepository.save(newToken);
        }
    }

    /**
     * 사용자의 알림 설정 상태를 확인합니다. (이 메서드는 원래 람다를 사용하지 않았습니다)
     */
    @Transactional(readOnly = true)
    public NotificationSettingStatusResponse getNotificationSettingStatus(Long userId) {
        User user = findUserById(userId);
        boolean needsSetup = !userNotificationSettingRepository.existsByUser(user);

        if (needsSetup) {
            return new NotificationSettingStatusResponse("NEEDS_SETUP");
        } else {
            return new NotificationSettingStatusResponse("CONFIGURED");
        }
    }

    /**
     * 신규 또는 기존 사용자의 알림 설정을 초기화합니다. (람다 제거 버전)
     */
    public void initializeNotificationSettings(Long userId, boolean agreed) {
        User user = findUserById(userId);

        if (userNotificationSettingRepository.existsByUser(user)) {
            return;
        }

        user.updateAgreedToPushNotification(agreed);
        List<UserNotificationSetting> settings = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            UserNotificationSetting setting = UserNotificationSetting.builder()
                    .user(user)
                    .notificationType(type)
                    .enabled(agreed)
                    .build();
            settings.add(setting);
        }

        userNotificationSettingRepository.saveAll(settings);
    }

    /**
     * 사용자의 실제 OS 푸시 권한 상태를 서버와 동기화합니다. (이 메서드는 원래 람다를 사용하지 않았습니다)
     */
    public void syncPushAgreement(Long userId, boolean osPermissionGranted) {
        User user = findUserById(userId);
        user.updateAgreedToPushNotification(osPermissionGranted);
    }

    /**
     * 사용자의 현재 모든 카테고리별 알림 설정을 조회합니다. (람다 제거 버전)
     */
    @Transactional(readOnly = true)
    public NotificationSettingListResponse getNotificationSettings(Long userId) {
        User user = findUserById(userId);
        List<UserNotificationSetting> settings = userNotificationSettingRepository.findAllByUser(user);

        List<NotificationSettingResponse> settingResponses = new ArrayList<>();
        for (UserNotificationSetting setting : settings) {
            NotificationSettingResponse response = new NotificationSettingResponse(
                    setting.getNotificationType(),
                    setting.isEnabled()
            );
            settingResponses.add(response);
        }

        return new NotificationSettingListResponse(settingResponses);
    }

    /**
     * ID로 사용자를 찾습니다. (람다 제거 버전)
     */
    private User findUserById(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            return optionalUser.get();
        } else {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 수신된 이벤트와 생성된 메시지를 바탕으로 Notification 엔티티를 생성하고 DB에 저장합니다.
     * @param recipient 알림을 받을 사용자 엔티티
     * @param event     알림 이벤트 데이터 (ID 값들을 담고 있음)
     * @param message   최종적으로 생성된 메시지 문자열
     */
    @Transactional
    public void createAndSaveNotification(User recipient,
                                          User actor,
                                          NotificationEvent event,
                                          String message) {

        Notification notification = Notification.builder()
                .user(recipient)
                .message(message)
                .notificationType(event.notificationType())
                .referenceId(event.referenceId())
                .actor(actor)
                .subReferenceId(event.commentId())
                .build();

        notificationRepository.save(notification);
    }
    /**
     * 특정 알림을 읽음 상태로 변경합니다.
     * @param userId         현재 로그인한 사용자의 ID
     * @param notificationId 읽음 처리할 알림의 ID
     */
    public void markNotificationAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notification.markAsRead();

    }

    public NotificationSliceResponseDto getNotifications(Long userId, NotificationType type, Pageable pageable) {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        Page<Notification> notificationPage = notificationRepository.findNotificationsByUserId(
                userId, sevenDaysAgo, type, pageable
        );

        List<Notification> notifications = notificationPage.getContent();
        if (notifications.isEmpty()) {
            return NotificationSliceResponseDto.builder()
                    .notifications(List.of())
                    .hasNext(false)
                    .build();
        }

        List<Long> actorIds = notifications.stream()
                .map(n -> n.getActor().getId())
                .distinct()
                .toList();

        Map<Long, Image> firstImageMap = imageRepository.findByImageTypeAndRelatedIdIn(ImageType.USER, actorIds)
                .stream()
                .collect(Collectors.toMap(
                        Image::getRelatedId,
                        image -> image,
                        (img1, img2) -> img1.getOrderIndex() < img2.getOrderIndex() ? img1 : img2
                ));

        List<NotificationResponseDto> dtos = notifications.stream()
                .map(notification -> {
                    User actor = notification.getActor();
                    Image profileImage = firstImageMap.get(actor.getId());
                    String profileImageUrl = (profileImage != null) ? profileImage.getUrl() : null;

                    ActorDto actorDto = ActorDto.builder()
                            .id(actor.getId())
                            .name(actor.getFirstName())
                            .profileImageUrl(profileImageUrl)
                            .build();

                    return NotificationResponseDto.builder()
                            .notificationId(notification.getId())
                            .message(notification.getMessage())
                            .isRead(notification.isRead())
                            .createdAt(notification.getCreatedAt())
                            .actor(actorDto)
                            .referenceId(notification.getReferenceId())
                            .subReferenceId(notification.getSubReferenceId())
                            .build();
                })
                .toList();

        String nextCursor = null;
        if (notificationPage.hasNext()) {
            nextCursor = notifications.get(notifications.size() - 1).getCreatedAt().toString();
        }

        return NotificationSliceResponseDto.builder()
                .notifications(dtos)
                .hasNext(notificationPage.hasNext())
                .nextCursor(nextCursor)
                .build();
    }
}