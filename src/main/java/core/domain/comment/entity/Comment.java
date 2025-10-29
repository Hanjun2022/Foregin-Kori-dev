package core.domain.comment.entity;

import core.domain.post.entity.Post;
import core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "comment",
        indexes = {
                @Index(name = "idx_comment_post", columnList = "post_id"),
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comments_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean anonymous;

    @Column(nullable = false)
    private boolean deleted = false;

    private Instant deletedAt;
    private String deletedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    public static Comment createRootComment(Post post, User author, String content, boolean anonymous) {
        validate(post, author, content);
        Comment c = new Comment();
        c.post = post;
        c.author = author;
        c.content = content;
        c.anonymous = anonymous;
        return c;
    }

    public static Comment createReplyComment(Post post, User author, String content, boolean anonymous, Comment parent) {
        validate(post, author, content);
        if (parent == null) throw new IllegalArgumentException("parent must not be null");
        if (!parent.getPost().getId().equals(post.getId())) {
            throw new IllegalArgumentException("Parent comment must belong to the same post");
        }
        Comment c = new Comment();
        c.post = post;
        c.author = author;
        c.content = content;
        c.anonymous = anonymous;
        c.setParent(parent);
        return c;
    }

    private static void validate(Post post, User author, String content) {
        if (post == null) throw new IllegalArgumentException("post must not be null");
        if (author == null) throw new IllegalArgumentException("author must not be null");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("content must not be blank");
    }

    public void markDeleted(String deleter) {
        this.deleted = true;
        this.deletedAt = Instant.from(LocalDateTime.now());
        this.deletedBy = deleter;
    }

    public boolean isLeaf() { return children == null || children.isEmpty(); }

    public void setParent(Comment parent) {
        this.parent = parent;
        parent.children.add(this);
    }

    public void changeContent(String content) {
        this.content = content;
    }
}
