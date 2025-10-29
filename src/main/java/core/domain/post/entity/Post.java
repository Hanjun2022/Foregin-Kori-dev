package core.domain.post.entity;

import core.domain.board.entity.Board;
import core.domain.comment.entity.Comment;
import core.domain.post.dto.PostWriteForChatRequest;
import core.domain.post.dto.PostWriteRequest;
import core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "post",
        indexes = {
                @Index(
                        name = "idx_post_board_created_id",
                        columnList = "board_id, created_at DESC, post_id DESC"
                ),
                @Index(
                        name = "idx_post_created_id",
                        columnList = "created_at DESC, post_id DESC"
                )
        }
)
@Getter
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "post_content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean anonymous;

    @Column(name = "check_count", nullable = false)
    private Long checkCount = 0L;

    @OneToMany(mappedBy = "post", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    public Post(PostWriteRequest request, User author, Board board) {

        this.author = author;
        this.board = board;
        this.content = request.content();
        this.anonymous = request.isAnonymous() != null ? request.isAnonymous() : false;
        this.checkCount = 0L;
    }

    public Post(PostWriteForChatRequest request, User user, Board board) {
        this.author=user;
        this.board = board;
        this.content = request.content();
        this.anonymous = false;
        this.checkCount = 0L;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeCheckCount() {
        this.checkCount = this.checkCount + 1;
    }
}
