package core.domain.comment.repository;

import core.domain.comment.entity.Comment;
import core.global.enums.LikeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;

public interface CommentRepositoryCustom {
    Slice<Comment> findByPostId(Long userId, Long postId, Pageable pageable);

    Slice<Comment> findCommentByCursor(
            Long userId,
            Long postId,
            Instant cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    );

    Slice<Comment> findPopularByPostId(
            Long userId,
            Long postId,
            LikeType type,
            Pageable pageable
    );

    Slice<Comment> findPopularByCursor(
            Long userId,
            Long postId,
            LikeType type,
            Long cursorLikeCount,
            Instant cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    );
}
