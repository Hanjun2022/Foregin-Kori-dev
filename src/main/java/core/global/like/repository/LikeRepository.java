package core.global.like.repository;

import core.global.enums.LikeType;
import core.global.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    @Query("select l from Like l where l.relatedId= :id and l.type= :likeType and l.user.email= :email")
    Optional<Like> findLikeByUserEmailAndType(@Param("email") String email, @Param("id") Long id, @Param("likeType") LikeType likeType);

    @Query("""
            select l.relatedId, count(l.id)
            from Like l
            where l.type = :type
              and l.relatedId in :ids
            group by l.relatedId
            """)
    List<Object[]> countByRelatedIds(
            @Param("type") LikeType type,
            @Param("ids") List<Long> ids
    );

    @Modifying
    @Query("delete from Like l where l.user.email = :email and l.type = :likeType and l.relatedId = :id")
    void deleteByUserEmailAndIdAndType(String email, Long id, LikeType likeType);

    @Query("""
        select l.relatedId
        from Like l
        where l.type = :type
          and l.user.email = :email
          and l.relatedId in :ids
    """)
    List<Long> findMyLikedRelatedIdsByEmail(@Param("email") String email,
                                            @Param("type") LikeType type,
                                            @Param("ids") Collection<Long> ids);

    @Query("select l.relatedId " +
           "from Like l " +
           "where l.user.id = :userId and l.type = :type and l.relatedId in :ids")
    List<Long> findMyLikedRelatedIds(@Param("userId") Long userId,
                                     @Param("type") LikeType type,
                                     @Param("ids") Collection<Long> ids);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
