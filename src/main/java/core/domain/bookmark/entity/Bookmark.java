package core.domain.bookmark.entity;

import core.domain.post.entity.Post;
import core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bookmark",
        uniqueConstraints = @UniqueConstraint(name = "uk_bookmark_user_post", columnNames = {"user_id", "post_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private Bookmark(User user, Post post) {
        this.user = user;
        this.post = post;
    }

    public static Bookmark createBookmark(User user, Post post) {
        return new Bookmark(user, post);
    }

}
