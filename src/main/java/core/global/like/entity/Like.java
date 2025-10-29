package core.global.like.entity;

import core.domain.user.entity.User;
import core.global.enums.LikeType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "likes",
        indexes = {
                @Index(name = "idx_likes_type_related", columnList = "type, related_id")
        }
)
@Getter
@NoArgsConstructor
public class Like {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "likes_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", nullable = false)
    private LikeType type;

    @Column(name = "related_id")
    private Long relatedId;

    @Builder
    Like(User user, LikeType type, Long relatedId) {
        this.user = user;
        this.type = type;
        this.relatedId = relatedId;
    }
}
