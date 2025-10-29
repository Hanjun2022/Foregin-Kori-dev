package core.domain.user.entity;


import core.global.enums.FollowStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follow")
@Getter
@NoArgsConstructor
public class Follow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FollowStatus status;

    @Builder
    public Follow(User user, User following, FollowStatus status) {
        this.user = user;
        this.following = following;
        this.status = status;
    }

    public void accept() {
        this.status = FollowStatus.ACCEPTED;
    }
}