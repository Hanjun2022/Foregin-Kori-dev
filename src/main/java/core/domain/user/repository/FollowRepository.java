package core.domain.user.repository;


import core.domain.user.entity.Follow;
import core.domain.user.entity.User;
import core.global.enums.FollowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface FollowRepository extends JpaRepository<Follow,Long> {
    /**
     기존 메서드: 내가 팔로우하는 사람들을 조회 (보낸사람 조회)
     */
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.user = :user AND f.status = :status")
    List<Follow> findByUserAndStatus(@Param("user") User user, @Param("status") FollowStatus status);

    @Query("SELECT f FROM Follow f " +
            "WHERE f.user.id = :userId  " +
            "AND f.status IN (:statuses)")
    List<Follow> findSentFollowsByStatuses(@Param("userId") Long userId,
                                           @Param("statuses") List<FollowStatus> statuses);

    /*
        새로운 메서드: 나를 팔로우하는 사람들을 조회 (받은 사람 조회)
     */
    @Query("SELECT f FROM Follow f JOIN FETCH f.user WHERE f.following = :user AND f.status = :status")
    List<Follow> findByFollowingAndStatus(@Param("user") User user, @Param("status") FollowStatus status);

    List<Follow> findAllByFollowingAndStatus(User following, FollowStatus status);

    /**
    특정 사용자와 팔로우 대상의 관계를 조회 (상태 무관)
     **/
    Optional<Follow> findByUserAndFollowing(User user, User following);

    Optional<Follow> findByUser_IdAndFollowing_IdAndStatus(Long userId, Long followingId, FollowStatus status);
    /**
     * 특정 사용자와 관련된 모든 팔로우 관계를 삭제합니다.
     * (해당 사용자가 팔로우한 경우 + 해당 사용자를 팔로우한 경우 모두)
     * @param userId 삭제할 사용자의 ID
     */
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.user.id = :userId OR f.following.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Follow f " +
            "WHERE (f.user.id = :userId OR f.following.id = :userId) " +
            "AND f.status = :status")
    List<Follow> findAllAcceptedFollowsByUserId(@Param("userId") Long userId,
                                                @Param("status") FollowStatus status);

    long countByUserIdAndStatus(Long userId, FollowStatus status);

    long countByFollowingIdAndStatus(Long followingId, FollowStatus status);
    /**
     * [기존 메서드 - 팔로우 수락/거절 등에 사용]
     * 특정 두 사용자 사이의 특정 상태를 가진 Follow 엔티티를 조회합니다.
     * @param user 팔로우를 신청한 사용자 (follower)
     * @param following 팔로우 신청을 받은 사용자 (followee)
     * @param status 조회할 팔로우 상태 (예: PENDING)
     * @return Optional<Follow>
     */
    Optional<Follow> findByUserAndFollowingAndStatus(User user, User following, FollowStatus status);


    /**
     * [새로운 메서드 - 친구 추천 필터링에 사용]
     * 특정 사용자가 팔로우 요청을 보냈거나(PENDING) 이미 친구 관계(ACCEPTED)인
     * 모든 다른 사용자의 ID를 조회합니다.
     * @param userId 팔로워의 ID (meId)
     * @param statuses 추천에서 제외할 팔로우 상태 목록
     * @return 추천에서 제외해야 할 사용자 ID Set
     */
    @Query("SELECT f.following.id FROM Follow f " +
            "WHERE f.user.id = :userId AND f.status IN :statuses")
    Set<Long> findFollowingIdsByUserId(@Param("userId") Long userId, @Param("statuses") List<FollowStatus> statuses);
}