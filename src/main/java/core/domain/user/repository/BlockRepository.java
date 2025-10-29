package core.domain.user.repository;

import core.domain.user.entity.BlockUser;
import core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BlockRepository extends JpaRepository<BlockUser, Long> {

    @Query("select b.blocked from BlockUser b " +
           "where b.user.email = :email")
    List<User> getBlockUsersByUserEmail(@Param("email") String email);

    @Query("select count(b) > 0 from BlockUser b " +
           "where b.user.id = :myId and b.blocked.id = :counterId")
    boolean existsBlock(@Param("myId") Long myId, @Param("counterId") Long counterId);

    @Query("SELECT b FROM BlockUser b WHERE b.user = :user AND b.blocked = :blockedUser")
    Optional<BlockUser> findBlockRelationship(@Param("user") User user, @Param("blockedUser") User blockedUser);

    List<BlockUser> findByUser(User user);
    @Query("select count(b) > 0 from BlockUser b " +
            "where b.user.email = :email and b.blocked.email = :email")
    boolean existsBlockedByEmail(@Param("email") String  email, @Param("email") String authorEmail);
    List<BlockUser> findByUserId(Long userId);

    @Modifying
    @Query("delete from BlockUser b where b.user = :user or b.blocked = :user")
    void deleteAllByUserOrBlocked(@Param("user") User user);

    /**
     * 특정 사용자와 관련된 모든 차단 관계의 상대방 ID를 조회합니다.
     * 1. 내가 다른 사람을 차단한 경우 (나는 'user', 상대방은 'blocked')
     * 2. 다른 사람이 나를 차단한 경우 (나는 'blocked', 상대방은 'user')
     * @param userId 기준 사용자의 ID (meId)
     * @return 차단 관계에 있는 모든 상대방 사용자 ID Set
     */
    @Query("SELECT b.blocked.id FROM BlockUser b WHERE b.user.id = :userId " +
            "UNION " +
            "SELECT b.user.id FROM BlockUser b WHERE b.blocked.id = :userId")
    Set<Long> findAllBlockedUserIds(@Param("userId") Long userId);
}
