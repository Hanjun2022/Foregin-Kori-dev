package core.domain.usernotificationsetting.service;

import core.domain.notification.dto.NotificationSettingListResponse;
import core.domain.notification.dto.NotificationSettingResponse;
import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import core.domain.usernotificationsetting.dto.NotificationSettingBulkUpdateRequestDto;
import core.domain.usernotificationsetting.dto.NotificationSettingInitItem;
import core.domain.usernotificationsetting.dto.NotificationSettingInitRequestDto;
import core.domain.usernotificationsetting.dto.NotificationSettingResponseDto;
import core.domain.usernotificationsetting.entity.UserNotificationSetting;
import core.domain.usernotificationsetting.repository.UserNotificationSettingRepository;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNotificationSettingService {

    private final UserNotificationSettingRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationSettingResponseDto> getUserNotificationSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_UNAUTHORIZED));

        List<UserNotificationSetting> settings = repository.findByUserId(user.getId());
        if (settings.isEmpty()) {
            throw new BusinessException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND);
        }

        return settings.stream()
                .map(s -> new NotificationSettingResponseDto(s.getNotificationType(), s.isEnabled()))
                .toList();
    }

    @Transactional
    public NotificationSettingListResponse updateUserNotificationSettings(
            Long userId, NotificationSettingBulkUpdateRequestDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<NotificationSettingResponse> responses = new ArrayList<>();

        for (NotificationSettingBulkUpdateRequestDto.SettingItem item : request.settings()) {
            UserNotificationSetting setting = repository
                    .findByUserIdAndNotificationType(user.getId(), item.notificationType())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));

            setting.updateEnabled(item.enabled());

            NotificationSettingResponse response = new NotificationSettingResponse(
                    setting.getNotificationType(),
                    setting.isEnabled()
            );

            responses.add(response);
        }

        return new NotificationSettingListResponse(responses);
    }

    @Transactional
    public void initializeNotificationSettings(Long userId, NotificationSettingInitRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        for (NotificationSettingInitItem item : request.getSettings()) {
            UserNotificationSetting setting = repository
                    .findByUserIdAndNotificationType(userId, item.getNotificationType())
                    .orElse(new UserNotificationSetting(user, item.getNotificationType(), item.isEnabled()));

            setting.updateEnabled(item.isEnabled());
            repository.save(setting);
        }
    }
}
