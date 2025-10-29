package core.domain.post.repository;

import core.domain.post.entity.Post;
import core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> , PostRepositoryCustom{
    @Query("SELECT p.author FROM Post p WHERE p.id = :postId")
    Optional<User> findUserByPostId(Long postId);

    List<Post> findAllByAuthorId(Long authorId);

    @Modifying
    @Query("update Post p set p.checkCount = p.checkCount + 1 where p.id = :postId")
    int increaseViewCount(@Param("postId") Long postId);
}
