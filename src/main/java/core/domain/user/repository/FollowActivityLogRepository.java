package core.domain.user.repository;


import core.domain.user.entity.FollowActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * FollowActivityLog 엔티티에 대한 데이터 액세스를 처리하는 리포지토리입니다.
 */
@Repository
public interface FollowActivityLogRepository extends JpaRepository<FollowActivityLog, Long> {
}