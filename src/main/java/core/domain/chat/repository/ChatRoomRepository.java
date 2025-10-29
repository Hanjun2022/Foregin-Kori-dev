package core.domain.chat.repository;

import core.domain.chat.entity.ChatParticipant;
import core.domain.chat.entity.ChatRoom;
import core.global.enums.ChatParticipantStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.group = true AND LOWER(cr.roomName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ChatRoom> findGroupChatRoomsByKeyword(@Param("keyword") String keyword);
    List<ChatRoom> findTop10ByGroupTrueOrderByCreatedAtDesc();
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.group = true " +
            "ORDER BY SIZE(cr.participants) DESC")
    List<ChatRoom> findTopByGroupTrueOrderByParticipantCountDesc(int limit);
    List<ChatRoom> findTop10ByGroupTrueAndIdLessThanOrderByCreatedAtDesc(Long id);
    /**
     * 특정 사용자가 ACTIVE 상태로 참여하고 있는 채팅방 목록을 조회합니다.
     *
     * @param userId         사용자 ID
     * @param participantStatus 조회할 참여 상태 (ACTIVE)
     * @return ACTIVE 상태인 채팅방 목록
     */
    @Query("SELECT cr FROM ChatRoom cr " +
            "JOIN cr.participants cp " +
            "WHERE cp.user.id = :userId AND cp.status = :participantStatus " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM ChatParticipant cp2 " +
            "  WHERE cp2.chatRoom = cr AND cp2.user.provider = 'SYSTEM'" +
            ")")
    List<ChatRoom> findActiveHumanChatRoomsByUserId(@Param("userId") Long userId, @Param("participantStatus") ChatParticipantStatus participantStatus);

    @Query("SELECT cr FROM ChatRoom cr JOIN FETCH cr.participants p JOIN FETCH p.user WHERE cr.id = :roomId")
    Optional<ChatRoom> findByIdWithParticipantsAndUsers(@Param("roomId") Long roomId);

    @EntityGraph(attributePaths = {"participants", "participants.user"})
    @Query("""
        select cr
        from ChatRoom cr
        where cr.group = false
          and (
            select count(distinct cpA.user.id)
            from ChatParticipant cpA
            where cpA.chatRoom = cr
              and cpA.user.id in :userIds
          ) = 2
          and (
            select count(distinct cpB.user.id)
            from ChatParticipant cpB
            where cpB.chatRoom = cr
          ) = 2
    """)
    List<ChatRoom> findOneToOneRoomByParticipantIds(@Param("userIds") List<Long> userIds);
    List<ChatRoom> findAllByOwnerId(Long ownerId);

    @Query("SELECT cr FROM ChatRoom cr " +
            "JOIN cr.participants p1 " +
            "JOIN cr.participants p2 " +
            "WHERE cr.group = false " +
            "AND p1.user.id = :userId1 " +
            "AND p2.user.id = :userId2")
    Optional<ChatRoom> findOneToOneChatRoomByParticipants(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

}