package core.domain.post.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import core.domain.comment.entity.QComment;
import core.domain.post.dto.SearchResultView;
import core.domain.post.entity.QPost;
import core.domain.user.entity.QUser;
import core.global.enums.ImageType;
import core.global.enums.LikeType;
import core.global.image.entity.QImage;
import core.global.like.entity.QLike;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import static core.domain.post.entity.QPost.post;
import static core.global.like.entity.QLike.like;

@Repository
@RequiredArgsConstructor
public class PostSearchRepositoryCustomImpl implements PostSearchRepositoryCustom {

    private final static LikeType LIKE_TYPE_POST = LikeType.POST;


    private final JPAQueryFactory jpaQueryFactory;

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * 정확도 정렬 1종 + 키셋 커서(score, created_at, post_id) 역순
     * - boardId == null 이면 전체
     * - afterTime/afterId 없으면 첫 페이지
     * <p>
     * PGroonga 기준:
     * - WHERE: p.content &@~ query
     * - 점수 : pgroonga.score(tableoid, ctid)
     * - 쿼리 escape: pgroonga.query_escape(q)
     */
    @Override
    public List<SearchResultView> search(String q, Long userId, Long boardId, List<Long> blockedIds,
                                         Instant afterTime, Long afterId, int limit) {
        QPost p = post;
        QUser user = QUser.user;
        QImage uimg = new QImage("uimg_ps");   // 프로필 이미지
        QImage pi1 = new QImage("pi1_ps");    // 첫 본문 이미지 id min
        QImage pi2 = new QImage("pi2_ps");    // 첫 본문 이미지 url
        QImage pic = new QImage("pic_ps");    // 이미지 개수
        QComment c = QComment.comment;        // 댓글
        QLike l = like;              // 좋아요

        // --- PGroonga 매치/점수(Double) ---
        var match = Expressions.booleanTemplate(
                "function('pgroonga_match', {0}, {1}) = true",
                p.content, Expressions.constant(q)
        );
        NumberExpression<Double> score = Expressions.numberTemplate(
                Double.class,
                "function('pgroonga_score_of', {0})",
                p.id
        );

        // WHERE
        var where = new BooleanBuilder().and(match);
        if (boardId != null) where.and(p.board.id.eq(boardId));
        if (blockedIds != null && !blockedIds.isEmpty()) where.and(p.author.id.notIn(blockedIds));

        // ---- 키셋 커서 ----
        if (afterTime != null && afterId != null) {
            NumberExpression<Double> curScore = Expressions.numberTemplate(
                    Double.class,
                    "function('pgroonga_score_of', {0})",
                    Expressions.constant(afterId)
            );
            where.and(
                    score.lt(curScore)
                            .or(score.eq(curScore).and(p.createdAt.lt(afterTime)))
                            .or(score.eq(curScore).and(p.createdAt.eq(afterTime)).and(p.id.lt(afterId)))
            );
        }

        // 미리보기 200자
        StringExpression preview200 =
                Expressions.stringTemplate("function('left', {0}, 200)", p.content);

        // 익명 보호 authorName/authorId
        StringExpression authorNameExpr = new CaseBuilder()
                .when(p.anonymous.isTrue()).then("Anonymity")
                .otherwise(
                        p.author.lastName.coalesce("")
                                .concat(" ")
                                .concat(p.author.firstName.coalesce(""))
                );

        Expression<Long> authorIdExpr = new CaseBuilder()
                .when(p.anonymous.isTrue()).then(Expressions.nullExpression(Long.class))
                .otherwise(p.author.id);

        // 프로필 이미지 URL (익명이면 null)
        Expression<String> userImageUrlExpr = new CaseBuilder()
                .when(p.anonymous.isTrue()).then(Expressions.nullExpression(String.class))
                .otherwise(
                        JPAExpressions.select(uimg.url)
                                .from(uimg)
                                .where(
                                        uimg.imageType.eq(ImageType.USER)
                                                .and(uimg.relatedId.eq(p.author.id))
                                )
                );

        // 본문 첫 이미지 URL (최소 id)
        Expression<String> contentThumbUrlExpr =
                JPAExpressions
                        .select(pi2.url)
                        .from(pi2)
                        .where(
                                pi2.imageType.eq(ImageType.POST),
                                pi2.relatedId.eq(p.id),
                                pi2.id.eq(
                                        JPAExpressions
                                                .select(pi1.id.min())
                                                .from(pi1)
                                                .where(
                                                        pi1.imageType.eq(ImageType.POST),
                                                        pi1.relatedId.eq(p.id)
                                                )
                                )
                        );

        // 이미지 개수
        Expression<Integer> imageCountExpr =
                JPAExpressions
                        .select(pic.id.countDistinct().intValue())
                        .from(pic)
                        .where(
                                pic.imageType.eq(ImageType.POST)
                                        .and(pic.relatedId.eq(p.id))
                        );

        // likeCount / commentCount 실제 집계
        Expression<Long> likeCountExpr =
                JPAExpressions.select(l.count())
                        .from(l)
                        .where(
                                l.type.eq(LikeType.POST)
                                        .and(l.relatedId.eq(p.id))
                        );

        Expression<Boolean> likedByMe = likedByViewerId(userId);

        Expression<Long> commentCountExpr =
                JPAExpressions.select(c.count())
                        .from(c)
                        .where(c.post.eq(p));

        // BoardItem.score(Long)용 반올림 점수(Long)
        NumberExpression<Long> scoreRounded =
                Expressions.numberTemplate(Long.class, "cast(round({0}, 0) as long)", score);

        return jpaQueryFactory
                .select(Projections.constructor(SearchResultView.class,
                        Projections.constructor(core.domain.board.dto.BoardItem.class,
                                // ★ BoardItem 생성자 시그니처 순서에 맞춰 실제 값 전달 ★
                                p.id,                 // id
                                preview200,           // preview
                                authorIdExpr,         // authorId (익명시 null)
                                authorNameExpr,       // authorName (익명시 "Anonymity")
                                p.board.category,     // category
                                p.createdAt,          // createdAt
                                likedByMe,
                                likeCountExpr,        // likeCount (실제)
                                commentCountExpr,     // commentCount (실제)
                                p.checkCount,         // viewCount (실제)
                                userImageUrlExpr,     // userImageUrl (익명시 null)
                                contentThumbUrlExpr,  // contentThumbnailUrl (실제)
                                imageCountExpr,       // imageCount (실제)
                                scoreRounded          // score(Long, 실제)
                        ),
                        score // SearchResultView(item, score(Double 원본))
                ))
                .from(p)
                .where(where)
                .orderBy(score.desc(), p.createdAt.desc(), p.id.desc())
                .limit(limit)
                .fetch();
    }

    private Expression<Boolean> likedByViewerId(Long viewerId) {
        if (viewerId == null) return Expressions.FALSE; // 비로그인
        return JPAExpressions
                .selectOne()
                .from(like)
                .where(
                        like.type.eq(LIKE_TYPE_POST)
                                .and(like.relatedId.eq(post.id))
                                .and(like.user.id.eq(viewerId))
                )
                .exists();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findHotKeywordsOrTitles(int topN) {
        String sql = """
                WITH docs AS (
                  SELECT p.post_id AS doc_id, p.post_content
                  FROM public.post p
                  WHERE p.created_at >= now() - interval '90 days'
                ),
                tokens AS (
                  SELECT d.doc_id, w.term
                  FROM docs d
                  CROSS JOIN LATERAL unnest(
                    pgroonga_tokenize(
                      d.post_content,
                      'tokenizer','TokenDelimit'     -- ★ 단어 단위 토크나이저
                    )
                  ) AS t(token_json)
                  /* term을 LATERAL 서브셀렉트에서 컬럼으로 만든다 */
                  CROSS JOIN LATERAL (
                    SELECT lower(btrim((t.token_json::jsonb ->> 'value'))) AS term
                  ) AS w
                  WHERE w.term <> ''
                    AND w.term !~ '\\s'               -- 공백 포함 토큰 제거
                    AND length(w.term) BETWEEN 2 AND 20
                    AND w.term !~ '^[0-9]+$'
                    AND w.term !~ '^(https?://|www\\\\.)'
                    AND w.term !~ '^[[:punct:]]+$'
                )
                SELECT term
                FROM tokens
                GROUP BY term
                HAVING COUNT(DISTINCT doc_id) >= 1   -- 문서 수 기준 빈도
                ORDER BY COUNT(DISTINCT doc_id) DESC
                LIMIT :topN
                """;


        return entityManager.createNativeQuery(sql)
                .setParameter("topN", topN)
                .getResultList();
    }


    /**
     * 자동완성:
     * - prefix: pgroonga.query_escape(q) || '*'
     * - 중복 스니펫 제거: GROUP BY snippet
     * - 정렬: max(score) desc, max(created_at) desc
     */
    @Override
    public List<String> suggest(String q, Long boardId, List<Long> blockedIds, int limit) {
        QPost p = post;

        // 접두어 매칭: 래퍼가 escape + '*'까지 처리
        var match = Expressions.booleanTemplate(
                "function('pgroonga_match_prefix', {0}, {1}) = true",
                p.content, Expressions.constant(q)
        );

        var where = new BooleanBuilder().and(match);
        if (boardId != null) where.and(p.board.id.eq(boardId));
        if (blockedIds != null && !blockedIds.isEmpty()) where.and(p.author.id.notIn(blockedIds));

        // 중복 제거 키: 140자 스니펫
        var snippet140 = Expressions.stringTemplate("left({0}, 140)", p.content);

        // 점수/최신일 집계 (tableoid/ctid 쓰지 말고 래퍼 사용)
        NumberExpression<Double> maxScore = Expressions.numberTemplate(
                Double.class, "max(function('pgroonga_score_of', {0}))", p.id
        );
        var maxCreatedAt = Expressions.dateTimeTemplate(Instant.class, "max({0})", p.createdAt);

        return jpaQueryFactory.select(snippet140)
                .from(p)
                .where(where)
                .groupBy(snippet140)
                .orderBy(maxScore.desc(), maxCreatedAt.desc())
                .limit(limit)
                .fetch();
    }
}