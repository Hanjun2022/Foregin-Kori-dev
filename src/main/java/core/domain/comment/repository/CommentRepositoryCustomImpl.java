package core.domain.comment.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import core.domain.comment.entity.Comment;
import core.domain.comment.entity.QComment;
import core.domain.post.entity.QPost;
import core.domain.user.entity.QBlockUser;
import core.domain.user.entity.QUser;
import core.global.enums.LikeType;
import core.global.like.entity.QLike;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory query;

    private static final QComment c = QComment.comment;
    private static final QUser u = new QUser("u");
    private static final QPost p = QPost.post;
    private static final QBlockUser bu1 = new QBlockUser("bu1");
    private static final QBlockUser bu2 = new QBlockUser("bu2");
    private static final QLike l = QLike.like;

    // ───────────────────────── 최신 ─────────────────────────

    @Override
    public Slice<Comment> findByPostId(Long userId, Long postId, Pageable pageable) {
        List<Comment> rows = query
                .selectFrom(c)
                .join(c.author, u).fetchJoin()
                .join(c.post, p).fetchJoin()
                .where(
                        p.id.eq(postId)
                                .and(visibleTo(userId))
                )
                .orderBy(c.createdAt.desc(), c.id.desc())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        return toSlice(rows, pageable);
    }

    @Override
    public Slice<Comment> findCommentByCursor(
            Long userId, Long postId, Instant cursorCreatedAt, Long cursorId, Pageable pageable
    ) {
        BooleanExpression ltCursor = c.createdAt.lt(cursorCreatedAt)
                .or(c.createdAt.eq(cursorCreatedAt)
                        .and(cursorId != null ? c.id.lt(cursorId) : Expressions.FALSE));

        List<Comment> rows = query
                .selectFrom(c)
                .join(c.author, u).fetchJoin()
                .join(c.post, p).fetchJoin()
                .where(
                        p.id.eq(postId)
                                .and(ltCursor)
                                .and(visibleTo(userId))
                )
                .orderBy(c.createdAt.desc(), c.id.desc())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        return toSlice(rows, pageable);
    }

    // ───────────────────────── 인기(좋아요 desc, createdAt desc, id desc) ─────────────────────────
    @Override
    public Slice<Comment> findPopularByPostId(
            Long userId, Long postId, LikeType type, Pageable pageable
    ) {
        NumberExpression<Long> lc = likeCount(type);

        List<Comment> rows = query
                .selectFrom(c)
                .join(c.author, u).fetchJoin()
                .join(c.post, p).fetchJoin()
                .where(
                        p.id.eq(postId)
                                .and(visibleTo(userId))
                )
                .orderBy(lc.desc(), c.createdAt.desc(), c.id.desc())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        return toSlice(rows, pageable);
    }

    @Override
    public Slice<Comment> findPopularByCursor(
            Long userId,
            Long postId,
            LikeType type,
            Long cursorLikeCount,
            Instant cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    ) {
        NumberExpression<Long> lc = likeCount(type);

        // 커서: (좋아요수 desc, createdAt desc, id desc)
        BooleanExpression ltCursor =
                lc.lt(cursorLikeCount)
                        .or(
                                lc.eq(cursorLikeCount)
                                        .and(
                                                c.createdAt.lt(cursorCreatedAt)
                                                        .or(
                                                                c.createdAt.eq(cursorCreatedAt)
                                                                        .and(cursorId != null ? c.id.lt(cursorId) : Expressions.FALSE)
                                                        )
                                        )
                        );

        List<Comment> rows = query
                .selectFrom(c)
                .join(c.author, u).fetchJoin()
                .join(c.post, p).fetchJoin()
                .where(
                        p.id.eq(postId)
                                .and(visibleTo(userId))
                                .and(ltCursor)
                )
                .orderBy(lc.desc(), c.createdAt.desc(), c.id.desc())
                .limit(pageable.getPageSize() + 1L)
                .fetch();

        return toSlice(rows, pageable);
    }

    /** 차단(양방향) 필터: userId가 null이면 필터 비활성화 */
    private BooleanExpression visibleTo(Long userId) {
        if (userId == null) return null;
        BooleanExpression notMyBlocking = JPAExpressions.selectOne().from(bu1)
                .where(bu1.user.id.eq(userId)
                        .and(bu1.blocked.id.eq(u.id)))
                .notExists();

        BooleanExpression notTheirBlocking = JPAExpressions.selectOne().from(bu2)
                .where(bu2.user.id.eq(u.id)
                        .and(bu2.blocked.id.eq(userId)))
                .notExists();

        return notMyBlocking.and(notTheirBlocking);
    }

    /** 인기 정렬용 좋아요 수 서브쿼리 */
    private NumberExpression<Long> likeCount(LikeType type) {
        return Expressions.numberTemplate(
                Long.class,
                "({0})",
                JPAExpressions.select(l.id.count())
                        .from(l)
                        .where(l.type.eq(type).and(l.relatedId.eq(c.id)))
        );
    }
    private <T> Slice<T> toSlice(List<T> rows, Pageable pageable) {
        boolean hasNext = rows.size() > pageable.getPageSize();
        if (hasNext) rows.remove(rows.size() - 1);
        return new SliceImpl<>(rows, pageable, hasNext);
    }

}