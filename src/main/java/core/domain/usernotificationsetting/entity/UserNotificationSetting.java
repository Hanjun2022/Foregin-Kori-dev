package core.domain.usernotificationsetting.entity;

import core.domain.user.entity.User;
import core.global.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_notification_setting", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "notification_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_notification_setting_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Builder
    public UserNotificationSetting(User user, NotificationType notificationType, boolean enabled) {
        this.user = user;
        this.notificationType = notificationType;
        this.enabled = enabled;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}