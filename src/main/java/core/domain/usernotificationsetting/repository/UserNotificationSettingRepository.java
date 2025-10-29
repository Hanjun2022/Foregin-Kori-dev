package core.domain.usernotificationsetting.repository;

import core.domain.user.entity.User;
import core.domain.usernotificationsetting.entity.UserNotificationSetting;
import core.global.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {
    List<UserNotificationSetting> findByUserId(Long userId);
    Optional<UserNotificationSetting> findByUserIdAndNotificationType(Long userId, NotificationType notificationType);

    List<UserNotificationSetting> findAllByUser(User user);

    boolean existsByUser(User user);
    void deleteAllByUserId(Long userId);
    boolean existsByUserIdAndNotificationTypeAndEnabledTrue(Long id, NotificationType type);

    Optional<UserNotificationSetting> findByUserAndNotificationType(User user, NotificationType notificationType);
}

