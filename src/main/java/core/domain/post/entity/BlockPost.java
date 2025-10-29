package core.domain.post.entity;

import core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "block_post",
        uniqueConstraints = @UniqueConstraint(name = "uk_block_post_user_post", columnNames = {"user_id", "post_id"})
)
@Getter
@NoArgsConstructor
public class BlockPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    public BlockPost(User me, Post post) {
        this.user = me;
        this.post = post;
    }
}
