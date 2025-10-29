package core.domain.comment.repository;

import core.domain.comment.dto.UserCommentItem;
import core.domain.comment.entity.Comment;
import core.domain.post.entity.Post;
import core.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    Boolean existsByParentIdAndDeletedFalse(Long commentId);

    Long countByParentId(Long id);

    @Query("""
                select c.post.id, count(c.id)
                from Comment c
                where c.post.id in :postIds
                group by c.post.id
            """)
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("""
            select new core.domain.comment.dto.UserCommentItem(c.id, p.id, p.content, c.content, c.createdAt)
            from Comment c
            join c.post p
            where c.author.email = :email
            and (:lastId is null or c.id < :lastId)
            order by c.id desc
            """)
    List<UserCommentItem> findMyCommentsForCursor(
            @Param("email") String email,
            @Param("lastId") Long lastId,
            Pageable pageable
    );
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.author.id = :userId")
    void deleteAllByAuthorId(@Param("userId") Long userId);
    void deleteAllByPostIn(List<Post> posts);


    @Query("SELECT c.author FROM Comment c WHERE c.id = :commentId")
    Optional<User> findUserByCommentId(Long commentId);
}
