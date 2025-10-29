package core.domain.chat.repository;

import core.domain.chat.entity.ChatParticipant;
import core.domain.chat.entity.ChatRoom;
import core.global.enums.ChatParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatRoomId(Long chatRoomId);

    Optional<ChatParticipant> findByChatRoomIdAndUserIdAndStatusIsNot(Long chatRoomId, Long userId, ChatParticipantStatus status);

    Optional<ChatParticipant> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);




    long countByChatRoomIdAndStatus(Long roomId, ChatParticipantStatus status);

    @Query(
        "SELECT cr FROM ChatRoom cr " +
                "WHERE cr.id IN (SELECT cp.chatRoom.id FROM ChatParticipant cp WHERE cp.user.id = :userId) " +
                "AND (" +
                "   (cr.group = true AND cr.roomName LIKE %:keyword%) " +
                "   OR " +
                "   (cr.group = false AND EXISTS (" +
                "       SELECT 1 FROM ChatParticipant cp2 " +
                "       JOIN cp2.user u " +
                "       WHERE cp2.chatRoom = cr AND cp2.user.id != :userId " +
                "       AND CONCAT(u.firstName, u.lastName) LIKE %:keyword%" +
                "   ))" +
                ")"
    )
    List<ChatRoom> findChatRoomsByUserIdAndRoomName(@Param("userId") Long userId, @Param("keyword") String keyword);
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);
    @Modifying
    @Query("DELETE FROM ChatParticipant p WHERE p.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);



    List<ChatParticipant> findAllByChatRoomIdAndUserIdNot(Long chatRoomId, Long userId);

}