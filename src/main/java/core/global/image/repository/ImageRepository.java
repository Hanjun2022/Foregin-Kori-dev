package core.global.image.repository;

import core.global.enums.ImageType;
import core.global.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    @Query("""
    select i.relatedId, i.url
    from Image i
    where i.imageType = :imageType
      and i.relatedId in :commentIds
    """)
    List<Object[]> findUrlByRelatedIds(
            @Param("imageType") ImageType imageType,
            @Param("commentIds") List<Long> commentIds
    );

    @Query("""
        select i.relatedId, i.url
        from Image i
        where i.id in (
            select min(i2.id)
            from Image i2
            where i2.imageType = :imageType
              and i2.relatedId in :relatedIds
            group by i2.relatedId
        )
    """)
    List<Object[]> findFirstUrlByRelatedIds(@Param("imageType") ImageType imageType,
                                            @Param("relatedIds") List<Long> relatedIds);

    @Query("""
        select i.relatedId, i.url
        from Image i
        where i.imageType = :imageType
          and i.relatedId in :relatedIds
        order by i.id asc
    """)
    List<Object[]> findAllUrlsByRelatedIds(@Param("imageType") ImageType imageType,
                                           @Param("relatedIds") List<Long> relatedIds);

    @Query("select i from Image i " +
           "where i.imageType = :type and i.relatedId = :relatedId " +
           "order by i.orderIndex asc")
    List<Image> findByImageTypeAndRelatedIdOrderByPositionAsc(
            @Param("type") ImageType type,
            @Param("relatedId") Long relatedId
    );

    @Modifying
    @Query("""
        delete from Image i
        where i.imageType = :imageType
          and i.relatedId  = :relatedId
          and i.url in :urls
    """)
    void deleteByImageTypeAndRelatedIdAndUrlIn(ImageType imageType, Long relatedId, Collection<String> urls);

    @Modifying
    @Query("""
        delete from Image i
         where i.imageType = :imageType
           and i.relatedId  = :relatedId
    """)
    void deleteByImageTypeAndRelatedId(@Param("imageType") ImageType imageType,
                                       @Param("relatedId") Long relatedId);

    void deleteByImageTypeAndRelatedIdAndUrlIn(ImageType imageType, Long relatedId, List<String> urls);

    List<Image> findByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType imageType, Long relatedId);

    Optional<Image> findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType imageType, Long relatedId);


    @Modifying
    @Query("DELETE FROM Image i WHERE i.imageType = :imageType AND i.relatedId = :relatedId")
    void deleteAllByImageTypeAndRelatedId(@Param("imageType") ImageType imageType, @Param("relatedId") Long relatedId);


    @Query("SELECT i FROM Image i WHERE i.imageType = :imageType AND i.relatedId IN :relatedIds AND i.orderIndex = 0")
    List<Image> findAllPrimaryImagesForUsers(@Param("imageType") ImageType imageType, @Param("relatedIds") List<Long> relatedIds);
    List<Image> findAllByImageTypeAndRelatedIdIn(ImageType imageType, List<Long> relatedIds);
    /**
     * [추가] 특정 타입과 ID에 해당하는 '모든' 이미지 목록을 조회합니다.
     * 채팅방의 기존 이미지를 모두 삭제하기 위해 사용됩니다.
     */
    List<Image> findByImageTypeAndRelatedId(ImageType imageType, Long relatedId);

    List<Image> findByImageTypeAndRelatedIdIn(ImageType imageType, List<Long> relatedIds);
}
