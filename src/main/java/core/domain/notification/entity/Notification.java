package core.domain.notification.entity;

import core.domain.user.entity.User;
import core.global.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "sub_reference_id")
    private Long subReferenceId;

    @Builder
    public Notification(User user,
                        String message,
                        Long referenceId,
                        NotificationType notificationType,
                        User actor,
                        Long subReferenceId) {
        this.user = user;
        this.message = message;
        this.referenceId = referenceId;
        this.notificationType = notificationType;
        this.actor = actor;
        this.subReferenceId = subReferenceId;
        this.createdAt = Instant.now();
        this.read = false;
    }

    /**
     * 알림을 '읽음' 상태로 변경하는 편의 메소드
     */
    public void markAsRead() {
        this.read = true;
    }
}