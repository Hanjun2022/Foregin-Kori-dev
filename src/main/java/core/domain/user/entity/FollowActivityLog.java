package core.domain.user.entity;


import core.global.enums.FollowActionType;
import core.global.enums.FollowStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "follow_activity_log")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FollowActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    // --- 기본 이벤트 정보 ---
    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private FollowActionType actionType;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "source")
    private String source;

    // --- 팔로워(Follower)의 스냅샷 정보 ---
    @Column(name = "follower_is_in_korea")
    private boolean followerIsInKorea;

    @Column(name = "follower_nationality")
    private String followerCountry;

    @Column(name = "follower_sex")
    private String followerSex;

    @Column(name = "follower_birth_date")
    private String followerBirthdate;

    @Column(name = "follower_language", length = 500)
    private String followerLanguage;

    // --- 팔로잉(Following)의 스냅샷 정보 ---
    @Column(name = "following_is_in_korea")
    private boolean followingIsInKorea;

    @Column(name = "following_nationality")
    private String followingCountry;

    @Column(name = "following_sex")
    private String followingSex;

    @Column(name = "following_birth_date")
    private String followingBirthdate;

    @Column(name = "following_language", length = 500)
    private String followingLanguage;


    @Builder
    public FollowActivityLog(Long followerId, Long followingId, FollowActionType actionType, String source,
                             boolean followerIsInKorea, String followerCountry, String followerSex, String followerBirthdate, String followerLanguage,
                             boolean followingIsInKorea, String followingCountry, String followingSex, String followingBirthdate, String followingLanguage) {
        this.followerId = followerId;
        this.followingId = followingId;
        this.actionType = actionType;
        this.source = source;

        this.followerIsInKorea = followerIsInKorea;
        this.followerCountry = followerCountry;
        this.followerSex = followerSex;
        this.followerBirthdate = followerBirthdate;
        this.followerLanguage = followerLanguage;

        this.followingIsInKorea = followingIsInKorea;
        this.followingCountry = followingCountry;
        this.followingSex = followingSex;
        this.followingBirthdate = followingBirthdate;
        this.followingLanguage = followingLanguage;
    }
}