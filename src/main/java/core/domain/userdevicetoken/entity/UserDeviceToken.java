package core.domain.userdevicetoken.entity;

import core.domain.user.entity.User;
import core.global.enums.DeviceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_device_token")
@Getter
@NoArgsConstructor
public class UserDeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserDeviceToken_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_token")
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UserDeviceToken(User user, String deviceToken) {
        this.user = user;
        this.deviceToken = deviceToken;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateUser(User user) {
        this.user = user;
        this.updatedAt = Instant.now();
    }
}