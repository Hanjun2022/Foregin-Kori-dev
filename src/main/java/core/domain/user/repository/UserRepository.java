package core.domain.user.repository;


import core.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndSocialId(String provider, String socialId);

    Optional<User> findByEmail(String email);

    Optional<User> findByName(String name);

    Optional<User> getUserById(Long id);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.id NOT IN :excludeIds " +
           "AND u.purpose IS NOT NULL AND u.purpose <> '' " +
           "AND u.country IS NOT NULL AND u.country <> '' " +
           "AND u.birthdate IS NOT NULL AND u.birthdate <> '' " +
           "AND u.language IS NOT NULL AND u.language <> ''")
    List<User> findFullProfiledRecommendationCandidates(@Param("excludeIds") Collection<Long> excludeIds);

    @Query(value = """
            SELECT
              SUM(CASE WHEN last_seen_at IS NOT NULL AND last_seen_at >= NOW() - INTERVAL '24 hour' THEN 1 ELSE 0 END) AS h_0_24,
              SUM(CASE WHEN last_seen_at <  NOW() - INTERVAL '24 hour'
                        AND last_seen_at >= NOW() - INTERVAL '72 hour' THEN 1 ELSE 0 END) AS h_24_72,
              SUM(CASE WHEN last_seen_at <  NOW() - INTERVAL '72 hour'
                        AND last_seen_at >= NOW() - INTERVAL '168 hour' THEN 1 ELSE 0 END) AS h_72_168,
              SUM(CASE WHEN last_seen_at <  NOW() - INTERVAL '168 hour'
                        AND last_seen_at >= NOW() - INTERVAL '336 hour' THEN 1 ELSE 0 END) AS h_168_336,
              SUM(CASE WHEN last_seen_at <  NOW() - INTERVAL '336 hour'
                        AND last_seen_at >= NOW() - INTERVAL '720 hour' THEN 1 ELSE 0 END) AS h_336_720,
              SUM(CASE WHEN last_seen_at IS NULL OR last_seen_at < NOW() - INTERVAL '720 hour' THEN 1 ELSE 0 END) AS h_720_inf
            FROM users
            """, nativeQuery = true)
    Object[] countInactiveBuckets();

    /**
     * 주차별 코호트: 해당 주에 가입한 유저의 최근 30일 활동 여부
     */
    @Query(value = """
            SELECT
              date_trunc('week', created_at)::date AS cohort_week,
              COUNT(*) AS cohort_total,
              SUM(CASE WHEN last_seen_at IS NOT NULL AND last_seen_at >= NOW() - INTERVAL '30 days' THEN 1 ELSE 0 END) AS active_recent_30d
            FROM users
            GROUP BY cohort_week
            ORDER BY cohort_week DESC
            LIMIT 26
            """, nativeQuery = true)
    List<Object[]> cohortRetention30d();

    /**
     * 최근 7일 last_seen_at 시간대(HOUR) 분포
     */
    @Query(value = """
            SELECT EXTRACT(HOUR FROM last_seen_at) AS hour_of_day, COUNT(*) AS cnt
            FROM users
            WHERE last_seen_at IS NOT NULL
              AND last_seen_at >= NOW() - INTERVAL '7 days'
            GROUP BY hour_of_day
            ORDER BY hour_of_day
            """, nativeQuery = true)
    List<Object[]> lastSeenHourDist7d();

    /**
     * 비활성(최근 30일 미접속) / 전체 사용자 수
     */
    @Query(value = """
  SELECT
    SUM(CASE WHEN last_seen_at IS NULL
              OR last_seen_at < NOW() - INTERVAL '30 day' THEN 1 ELSE 0 END) AS inactive_30d,
    COUNT(*) AS total_users
  FROM users
  """, nativeQuery = true)
    List<Object[]> countInactive30dAndTotal();

    @Query("SELECT u FROM User u")
    Stream<User> findAllAsStream();
}
