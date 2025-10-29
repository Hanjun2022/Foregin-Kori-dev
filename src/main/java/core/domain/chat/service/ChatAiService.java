package core.domain.chat.service;

import core.domain.chat.dto.*;
import core.domain.chat.entity.ChatMessage;
import core.domain.chat.entity.ChatParticipant;
import core.domain.chat.entity.ChatRoom;
import core.domain.chat.repository.ChatMessageRepository;
import core.domain.chat.repository.ChatParticipantRepository;
import core.domain.chat.repository.ChatRoomRepository;
import core.domain.user.entity.User;
import core.domain.user.repository.UserRepository;
import core.global.enums.ErrorCode;
import core.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatAiService {

    private static final Long AI_USER_ID = 1L;
    private static final int PAGE_SIZE = 50;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ClovaXService clovaXService;

    /**
     * AI와 1:1 채팅방 생성
     * 이미 존재하는 경우 해당 채팅방 정보를 반환합니다.
     */

    @Transactional
    public ChatAiRoomResponse createAiChatRoom(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }


        Optional<ChatRoom> existingRoomOptional = chatRoomRepository.findOneToOneChatRoomByParticipants(userId, AI_USER_ID);
        if (existingRoomOptional.isPresent()) {
            ChatRoom existingRoom = existingRoomOptional.get();
            return ChatAiRoomResponse.of(existingRoom, false);
        } else {
//            User user = findUserById(userId);
            User aiUser = findUserById(AI_USER_ID);

            ChatRoom newRoom = new ChatRoom(false, Instant.now(), "AI Chat");

            ChatParticipant userParticipant = new ChatParticipant(newRoom, user);
            ChatParticipant aiParticipant = new ChatParticipant(newRoom, aiUser);
            newRoom.addParticipant(userParticipant);
            newRoom.addParticipant(aiParticipant);

            ChatRoom savedRoom = chatRoomRepository.save(newRoom);
            return ChatAiRoomResponse.of(savedRoom, true);
        }
    }


    /**
     * AI 채팅방에 메시지 전송
     * 1. 사용자의 메시지를 DB에 저장
     * 2. 이전 대화내역을 포함하여 ClovaX API 호출
     * 3. AI의 응답을 DB에 저장하고 반환
     */
    @Transactional
    public AiMessageResponse sendMessageToAi(Long userId, Long roomId, AiMessageRequest request) {
        User sender = findUserById(userId);

        if(sender.getBirthdate()==null||sender.getPurpose()==null||sender.getIntroduction()==null||sender.getLanguage()==null||sender.getHobby()==null||sender.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }


        ChatRoom chatRoom = findChatRoomById(roomId);
        validateParticipant(userId, chatRoom);


        ChatMessage userMessage = new ChatMessage(chatRoom, sender, request.content());
        chatMessageRepository.save(userMessage);


        List<ChatMessage> recentMessages = chatMessageRepository.findTop10ByChatRoomIdOrderBySentAtDesc(roomId);

        Collections.reverse(recentMessages);

        List<ClovaXRequest.Message> chatHistory = recentMessages.stream()
                .map(msg -> ClovaXRequest.Message.builder()
                        .role(msg.getSender().getId().equals(AI_USER_ID) ? "assistant" : "user")
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());
        String aiContent = clovaXService.getAiResponse(chatHistory)
                .block()
                .getResult()
                .getMessage()
                .getContent();

        User aiUser = findUserById(AI_USER_ID);
        ChatMessage aiMessage = new ChatMessage(chatRoom, aiUser, aiContent);
        chatMessageRepository.save(aiMessage);

        return AiMessageResponse.from(aiMessage);
    }

    /**
     * 특정 AI 채팅방의 모든 메시지 내역 조회
     */
    @Transactional(readOnly = true)
    public List<AiMessageResponse> getChatMessages(Long userId, Long roomId) {
        ChatRoom chatRoom = findChatRoomById(roomId);
        validateParticipant(userId, chatRoom);

        return chatMessageRepository.findByChatRoomIdOrderBySentAtAsc(roomId).stream()
                .map(AiMessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * AI 채팅방과 관련된 모든 데이터(메시지, 참여자, 채팅방)를 영구적으로 삭제합니다.
     */
    @Transactional
    public void deleteAiChatRoom(Long userId, Long roomId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatRoom chatRoom = findChatRoomById(roomId);
        validateParticipant(userId, chatRoom);
        chatMessageRepository.deleteByChatRoomId(roomId);
        chatRoomRepository.delete(chatRoom);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private ChatRoom findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void validateParticipant(Long userId, ChatRoom chatRoom) {
        boolean isParticipant = chatRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.FORBIDDEN_MESSAGE_DELETE);
        }
    }
    /**
     * 특정 AI 채팅방의 메시지 내역을 무한 스크롤 방식으로 조회합니다.
     * @param userId 요청한 사용자 ID
     * @param roomId 조회할 채팅방 ID
     * @param lastMessageId 마지막으로 보였던 메시지의 ID (이 ID보다 오래된 메시지를 조회)
     * @return MessageSliceResponse - 메시지 목록과 다음 페이지 존재 여부
     */
    @Transactional(readOnly = true)
    public MessageSliceResponse getChatMessages(Long userId, Long roomId, Long lastMessageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatRoom chatRoom = findChatRoomById(roomId);
        validateParticipant(userId, chatRoom);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("id").descending());

        Slice<ChatMessage> messageSlice;
        if (lastMessageId == null) {
            messageSlice = chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageable);
        } else {
            messageSlice = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, lastMessageId, pageable);
        }

        List<AiMessageResponse> messageResponses = messageSlice.getContent().stream()
                .map(AiMessageResponse::from)
                .collect(Collectors.toList());

        return new MessageSliceResponse(messageResponses, messageSlice.hasNext());
    }
    /**
     * 사용자가 신고한 AI 메시지를 DB에서 삭제합니다.
     * @param userId 신고를 요청한 사용자 ID
     * @param messageId 삭제할 메시지 ID
     */
    @Transactional
    public void deleteReportedMessage(Long userId, Long messageId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        validateParticipant(userId, message.getChatRoom());
        if (!message.getSender().getId().equals(AI_USER_ID)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_MESSAGE_DELETE);
        }
        chatMessageRepository.delete(message);
    }
}