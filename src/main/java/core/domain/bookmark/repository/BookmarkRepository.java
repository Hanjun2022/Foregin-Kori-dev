package core.domain.bookmark.repository;

import core.domain.bookmark.entity.Bookmark;
import core.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    @EntityGraph(attributePaths = {"post", "post.author"})
    Slice<Bookmark> findByUserEmailOrderByIdDesc(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "post.author"})
    Slice<Bookmark> findByUserEmailAndIdLessThanOrderByIdDesc(String email, Long id, Pageable pageable);

    Optional<Bookmark> findByUserEmailAndPostId(String email, Long postId);

    @Modifying
    void deleteByUserEmailAndPostId(String email, Long postId);
    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    void deleteAllByPostIn(List<Post> posts);
}
