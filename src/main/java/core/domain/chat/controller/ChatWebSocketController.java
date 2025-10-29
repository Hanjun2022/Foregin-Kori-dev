package core.domain.chat.controller;

import core.domain.chat.dto.*;
import core.domain.chat.entity.ChatRoom;
import core.domain.chat.service.ChatService;
import core.global.config.CustomUserDetails;
import core.global.service.TranslationService;
import core.domain.user.repository.UserRepository;
import core.global.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations template;
    private final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);
    /**
     * @apiNote 새로운 메시지를 전송하고, 해당 채팅방의 구독자들에게 브로드캐스트합니다.
     *
     * @param req 전송 메시지 요청 (roomId, senderId, content, targetLanguage, translate)
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(SendMessageRequest req) {
        try {
            chatService.processAndSendChatMessage(req);

            log.info("메시지 및 요약 전송 성공: roomId={}, senderId={}", req.roomId(), req.senderId());
        } catch (Exception e) {
            log.error("메시지 전송 실패", e);
        }
    }
    /**
     * @apiNote 사용자가 메시지를 입력 중임을 알리는 이벤트를 다른 참여자에게 전송합니다.
     *
     * @param event 타이핑 이벤트 정보 (roomId, userId, isTyping)
     */
    @MessageMapping("/chat.typing")
    public void handleTypingEvent(@Payload TypingEvent event) {
        template.convertAndSend("/topic/chatrooms/" + event.roomId(), event);
    }

    /**
     * @apiNote 메시지 읽음 상태를 업데이트하고, 해당 채팅방의 다른 참여자에게 실시간으로 알립니다.
     * 1:1 채팅의 경우 상대방이 읽으면 readCount가 줄고, 그룹 채팅은 읽지 않은 사람 수가 줄어듭니다.
     *
     * @param req 읽음 상태 업데이트 요청 (roomId, readerId, lastReadMessageId)
     */

    @MessageMapping("/chat.markAsRead")
    public void markMessagesAsRead(@Payload MarkAsReadRequest req) {
        try {
            chatService.processMarkAsRead(req, req.userId());
        } catch (Exception e) {
            log.error("메시지 읽음 처리 중 오류 발생: {}", req, e);
        }
    }
    /**
     * @apiNote 메시지 삭제를 처리하고, 해당 채팅방의 모든 참여자에게 삭제 사실을 알립니다.
     *
     * @param req 삭제 요청 정보 (messageId, userId)
     */
    @MessageMapping("/chat.deleteMessage")
    public void deleteMessage(@Payload DeleteMessageRequest req) {
        try {
            chatService.deleteMessageAndBroadcast(req.messageId(), req.senderId());
            log.info("메시지 삭제 요청 처리: messageId={}, userId={}", req.messageId(), req.senderId());
        } catch (Exception e) {
            log.error("메시지 삭제 처리 중 에러 발생", e);
        }
    }
    @MessageMapping("/chat.sendMedia")
    public void sendMediaMessage(SendMediaMessageRequest req) {
        chatService.processAndSendMediaMessage(req);
    }
}