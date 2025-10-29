package core.domain.post.repository;

import core.domain.post.entity.BlockPost;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BlockPostRepository extends JpaRepository<BlockPost, Long> {

    @Query("select count(b)>0 from BlockPost b where b.user.id=:userId and b.post.id=:postId")
    boolean existsBlock(@Param("userId") Long userId, @Param("postId") Long postId);
    /**
     * 특정 사용자와 관련된 모든 게시물 차단 정보를 삭제합니다.
     * 1. 해당 사용자가 다른 게시물을 차단한 정보 (bp.user.id)
     * 2. 다른 사용자가 해당 사용자의 게시물을 차단한 정보 (bp.post.author.id)
     */
    @Modifying
    @Query("DELETE FROM BlockPost bp WHERE bp.user.id = :userId OR bp.post.author.id = :userId")
    void deleteAllBlockPostsRelatedToUser(@Param("userId") Long userId);

}
