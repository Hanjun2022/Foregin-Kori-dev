package core.domain.chat.service;


import core.domain.chat.dto.*;
import core.domain.chat.entity.ChatMessage;
import core.domain.chat.entity.ChatParticipant;
import core.domain.chat.entity.ChatRoom;
import core.domain.chat.repository.ChatMessageRepository;
import core.domain.chat.repository.ChatParticipantRepository;
import core.domain.chat.repository.ChatRoomRepository;
import core.domain.notification.dto.NotificationEvent;
import core.domain.user.entity.BlockUser;
import core.domain.user.entity.User;
import core.domain.user.repository.BlockRepository;
import core.domain.user.repository.UserRepository;
import core.global.enums.*;
import core.global.exception.BusinessException;
import core.global.image.entity.Image;
import core.global.image.repository.ImageRepository;
import core.global.image.service.ImageService;
import core.global.metrics.SocialChatMetrics;
import core.global.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private static final int MESSAGE_PAGE_SIZE = 20;
    private final ChatRoomRepository chatRoomRepo;
    private final ChatParticipantRepository participantRepo;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final TranslationService translationService;
    private final ImageRepository imageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate; // 주입 필요
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;
    private final BlockRepository blockRepository;
    private final S3Presigner s3Presigner;
    private final SocialChatMetrics socialChatMetrics;

    private String countryOf(User u) {
        return Optional.ofNullable(u.getCountry()).orElse(null); // null/빈값은 metrics에서 UNK 처리
    }

    @Value("${cdn.base-url}")
    private String cdnBaseUrl;

    @Value("${ncp.s3.bucket}")
    private String bucketName;

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getMyAllChatRoomSummaries(Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<ChatRoom> rooms = chatRoomRepo.findActiveHumanChatRoomsByUserId(userId, ChatParticipantStatus.ACTIVE);

        return rooms.stream()
                // 🚨 차단 필터링 로직
                .filter(room -> {
                    if (room.getGroup()) {
                        return true;
                    }
                    Optional<User> opponentOpt = room.getParticipants().stream()
                            .map(ChatParticipant::getUser)
                            .filter(u -> !u.getId().equals(userId))
                            .findFirst();

                    if (opponentOpt.isPresent()) {
                        User opponent = opponentOpt.get();
                        Long opponentId = opponent.getId();

                        boolean isBlockedByMe = blockRepository.existsBlock(userId, opponentId);

                        return !isBlockedByMe; // '내가 차단한 경우만 숨김'이 일반적
                    }

                    return true;
                })
                .map(room -> new ChatRoomWithTime(room, getLastMessageTime(room.getId())))
                .sorted(Comparator.comparing(
                        ChatRoomWithTime::lastMessageTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(roomWithTime -> {
                    ChatRoom room = roomWithTime.room();
                    Instant lastMessageTime = roomWithTime.lastMessageTime();

                    // 그룹 채팅방의 마지막 메시지는 차단된 유저 메시지를 제외
                    String lastMessageContent = getLastNonBlockedMessageContent(room.getId(), userId);
                    int unreadCount = countUnreadMessages(room.getId(), userId);
                    int participantCount = room.getParticipants().size();
                    String roomName;
                    String roomImageUrl;

                    if (!room.getGroup()) {
                        User opponent = room.getParticipants().stream()
                                .map(ChatParticipant::getUser)
                                .filter(u -> !u.getId().equals(userId))
                                .findFirst()
                                .orElse(null);

                        if (opponent != null) {
                            roomName = opponent.getFirstName() + " " + opponent.getLastName();
                            roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, opponent.getId())
                                    .map(Image::getUrl)
                                    .orElse(null);
                        } else {
                            roomName = "(알 수 없는 사용자)";
                            roomImageUrl = null;
                        }

                    } else {
                        roomName = room.getRoomName();
                        roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.CHAT_ROOM, room.getId())
                                .map(Image::getUrl)
                                .orElse(null);
                    }

                    return new ChatRoomSummaryResponse(
                            room.getId(),
                            roomName,
                            lastMessageContent,
                            lastMessageTime,
                            roomImageUrl,
                            unreadCount,
                            participantCount
                    );
                })
                .toList();
    }

    private String getLastNonBlockedMessageContent(Long roomId, Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<User> blockedUsers = blockRepository.findByUser(currentUser)
                .stream()
                .map(BlockUser::getBlocked)
                .toList();

        Optional<ChatMessage> lastMessage;

        if (blockedUsers.isEmpty()) {
            lastMessage = chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(roomId);
        } else {
            lastMessage = chatMessageRepository.findFirstByChatRoomIdAndSenderNotInOrderBySentAtDesc(roomId, blockedUsers);
        }

        return lastMessage.map(ChatMessage::getContent)
                .orElse("새로운 메시지가 없습니다.");
    }

    @Transactional
    public ChatRoom createRoom(Long currentUserId, Long otherUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        List<Long> userIds = Arrays.asList(currentUserId, otherUserId);
        List<ChatRoom> existingRooms = chatRoomRepo.findOneToOneRoomByParticipantIds(userIds);

        if (!existingRooms.isEmpty()) {
            if (existingRooms.size() > 1) {
                log.warn("중복된 1:1 채팅방 발견. 사용자 ID: {}, {}. 첫 번째 방을 사용합니다.", currentUserId, otherUserId);
            }
            ChatRoom room = existingRooms.get(0);
            return handleExistingRoom(room, currentUserId);
        } else {
            return createNewOneToOneChatRoom(currentUserId, otherUserId);
        }
    }

    private ChatRoom handleExistingRoom(ChatRoom room, Long currentUserId) {

        Optional<ChatParticipant> currentParticipant = room.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(currentUserId))
                .findFirst();
        if (currentParticipant.isPresent() && currentParticipant.get().getStatus() == ChatParticipantStatus.LEFT) {
            currentParticipant.get().reJoin();
        }

        return room;
    }

    private ChatRoom createNewOneToOneChatRoom(Long userId1, Long userId2) {
        User currentUser = userRepository.findById(userId1)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User otherUser = userRepository.findById(userId2)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom newRoom = new ChatRoom(false, Instant.now(), "1:1 채팅방");

        ChatParticipant participant1 = new ChatParticipant(newRoom, currentUser);
        ChatParticipant participant2 = new ChatParticipant(newRoom, otherUser);

        newRoom.addParticipant(participant1);
        newRoom.addParticipant(participant2);

        socialChatMetrics.recordInterest(
                countryOf(currentUser),
                countryOf(otherUser),
                "chat_room"
        );


        return chatRoomRepo.save(newRoom);
    }

    /**
     * 사용자가 채팅방을 나갑니다.
     * 1:1 채팅방의 경우, 상대방은 방에 남아있습니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 나가려는 사용자 ID
     * @return 채팅방을 나가는 데 성공했는지 여부
     */
    @Transactional
    public boolean leaveRoom(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatParticipant participant = participantRepo.findByChatRoomIdAndUserIdAndStatusIsNot(roomId, userId, ChatParticipantStatus.LEFT)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND));
        participant.leave();
        deleteRoomIfEmpty(roomId);
        return true;
    }

    /**
     * 채팅방의 모든 참여자가 나갔는지 확인하고, 비어있으면 삭제합니다.
     * 이 메서드는 leaveRoom()에서 호출되어 채팅방 삭제 로직을 분리합니다.
     *
     * @param roomId 확인할 채팅방 ID
     */
    @Transactional
    public void deleteRoomIfEmpty(Long roomId) {
        ChatRoom room = chatRoomRepo.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        long remainingActiveParticipants = participantRepo.countByChatRoomIdAndStatus(roomId, ChatParticipantStatus.ACTIVE);

        if (remainingActiveParticipants == 0) {
            chatMessageRepository.deleteByChatRoomId(roomId);
            chatRoomRepo.delete(room);
            // todo : 채팅방 내 동영상 사진 삭제 필요
        }
    }

    @Transactional(readOnly = true)
    public List<ChatRoomParticipantsResponse> getRoomParticipants(Long roomId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoom(chatRoom);
        return participants.stream()
                .map(p -> {
                    String userImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(
                                    ImageType.USER, p.getUser().getId())
                            .map(Image::getUrl)
                            .orElse(null);

                    boolean isHost = chatRoom.getOwner() != null && chatRoom.getOwner().getId().equals(p.getUser().getId());

                    return new ChatRoomParticipantsResponse(
                            p.getUser().getId(),
                            p.getUser().getFirstName(),
                            p.getUser().getLastName(),
                            userImageUrl,
                            isHost
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * @param roomId        채팅방 ID
     * @param userId        조회하는 사용자 ID
     * @param lastMessageId 마지막으로 조회된 메시지 ID (무한 스크롤용)
     * @return ChatMessage 엔티티 목록
     * @apiNote 채팅방 메시지를 무한 스크롤로 조회하는 핵심 로직입니다.
     * 이 메서드는 항상 ChatMessage 엔티티 목록을 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getRawMessages(Long roomId, Long userId, Long lastMessageId) {
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND));

        if (participant.getStatus() == ChatParticipantStatus.LEFT && participant.getLastLeftAt() != null) {
            Instant lastLeftAt = participant.getLastLeftAt();

            if (lastMessageId != null) {
                return chatMessageRepository.findByChatRoomIdAndSentAtAfterAndIdBefore(
                        roomId, lastLeftAt, lastMessageId,
                        PageRequest.of(0, MESSAGE_PAGE_SIZE, Sort.by("sentAt").descending())
                );
            } else {
                return chatMessageRepository.findByChatRoomIdAndSentAtAfter(
                        roomId, lastLeftAt,
                        PageRequest.of(0, MESSAGE_PAGE_SIZE, Sort.by("sentAt").descending())
                );
            }
        } else {
            if (lastMessageId != null) {
                return chatMessageRepository.findByChatRoomIdAndIdBefore(
                        roomId, lastMessageId,
                        PageRequest.of(0, MESSAGE_PAGE_SIZE, Sort.by("sentAt").descending())
                );
            } else {
                return chatMessageRepository.findByChatRoomId(
                        roomId,
                        PageRequest.of(0, MESSAGE_PAGE_SIZE, Sort.by("sentAt").descending())
                );
            }
        }
    }

    /**
     * @param roomId        채팅방 ID
     * @param userId        조회하는 사용자 ID
     * @param lastMessageId 마지막으로 조회된 메시지 ID (무한 스크롤용)
     * @return ChatMessageResponse 목록
     * @apiNote 채팅방 메시지를 조회하고, 번역 요청에 따라 ChatMessageResponse 목록을 반환합니다.
     * 이 메서드가 컨트롤러에서 호출되는 주된 엔드포인트가 됩니다.
     */

    @Transactional
    public List<ChatMessageResponse> getMessages(
            Long roomId,
            Long userId,
            Long lastMessageId

    ) {
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

        boolean needsTranslation = participant.isTranslateEnabled();
        String targetLanguage = participant.getUser().getTranslateLanguage();

        List<ChatMessage> messages = getRawMessages(roomId, userId, lastMessageId);

        List<Long> blockedIds = getBlockedUserIds(userId);
        if (!blockedIds.isEmpty()) {
            messages = messages.stream()
                    .filter(msg -> !blockedIds.contains(msg.getSender().getId()))
                    .toList();
        }

        if (needsTranslation && targetLanguage != null && !targetLanguage.isEmpty()) {
            List<String> originalContents = messages.stream()
                    .map(ChatMessage::getContent)
                    .collect(Collectors.toList());
            List<String> translatedContents = translationService.translateMessages(originalContents, targetLanguage);

            List<ChatMessage> finalMessages = messages;
            return IntStream.range(0, messages.size())
                    .mapToObj(i -> {
                        ChatMessage message = finalMessages.get(i);
                        String translatedContent = translatedContents.get(i);
                        User sender = message.getSender();
                        String senderImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                                .map(Image::getUrl)
                                .orElse(null);

                        return new ChatMessageResponse(
                                message.getId(),
                                message.getChatRoom().getId(),
                                sender.getId(),
                                message.getContent(),
                                translatedContent,
                                message.getSentAt(),
                                sender.getFirstName(),
                                sender.getLastName(),
                                senderImageUrl,
                                message.getMessageType()
                        );
                    }).collect(Collectors.toList());
        } else {
            return messages.stream()
                    .map(message -> {
                        User sender = message.getSender();
                        String senderImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                                .map(Image::getUrl)
                                .orElse(null);

                        return new ChatMessageResponse(
                                message.getId(),
                                message.getChatRoom().getId(),
                                sender.getId(),
                                message.getContent(),
                                null,
                                message.getSentAt(),
                                sender.getFirstName(),
                                sender.getLastName(),
                                senderImageUrl,
                                message.getMessageType()
                        );
                    }).collect(Collectors.toList());
        }
    }

    @Transactional
    public ChatMessage saveMessage(Long roomId, Long senderId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom room = chatRoomRepo.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatParticipant senderParticipant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND));

        if (senderParticipant.getStatus() == ChatParticipantStatus.LEFT) {
            senderParticipant.reJoin();
        }

        if (Boolean.FALSE.equals(room.getGroup())) {
            List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomId(roomId);
            for (ChatParticipant participant : participants) {
                if (!participant.getUser().getId().equals(senderId) && participant.getStatus() == ChatParticipantStatus.LEFT) {
                    participant.reJoin();
                }
            }
        }

        ChatMessage message = new ChatMessage(room, sender, content);
        return chatMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> searchMessages(Long roomId, Long userId, String keyword) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

        boolean needsTranslation = participant.isTranslateEnabled();
        String targetLanguage = participant.getUser().getTranslateLanguage();

        if (!needsTranslation || targetLanguage == null || targetLanguage.isEmpty()) {
            List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAndContentContaining(roomId, keyword);
            return messages.stream()
                    .map(message -> {
                        User sender = message.getSender();
                        String userImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                                .map(Image::getUrl)
                                .orElse(null);

                        return new ChatMessageResponse(
                                message.getId(),
                                message.getChatRoom().getId(),
                                sender.getId(),
                                message.getContent(),
                                null,
                                message.getSentAt(),
                                sender.getFirstName(),
                                sender.getLastName(),
                                userImageUrl,
                                message.getMessageType()
                        );
                    })
                    .sorted(Comparator.comparing(ChatMessageResponse::sentAt, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        } else {
            List<ChatMessage> allMessages = chatMessageRepository.findByChatRoomIdOrderByIdAsc(roomId);
            if (allMessages.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> originalContents = allMessages.stream().map(ChatMessage::getContent).collect(Collectors.toList());
            List<String> translatedContents = translationService.translateMessages(originalContents, targetLanguage);

            List<MessagePair> messagePairs = IntStream.range(0, allMessages.size())
                    .mapToObj(i -> new MessagePair(allMessages.get(i), translatedContents.get(i)))
                    .toList();

            return messagePairs.stream()
                    .filter(pair -> pair.translatedContent().toLowerCase().contains(keyword.toLowerCase()))
                    .map(pair -> {
                        User sender = pair.originalMessage().getSender();
                        String userImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                                .map(Image::getUrl)
                                .orElse(null);

                        return new ChatMessageResponse(
                                pair.originalMessage().getId(),
                                pair.originalMessage().getChatRoom().getId(),
                                sender.getId(),
                                pair.originalMessage().getContent(),
                                pair.translatedContent(),
                                pair.originalMessage().getSentAt(),
                                sender.getFirstName(),
                                sender.getLastName(),
                                userImageUrl,
                                pair.originalMessage().getMessageType()
                        );
                    })
                    .sorted(Comparator.comparing(ChatMessageResponse::sentAt, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        }
    }

    @Transactional
    public void processMarkAsRead(MarkAsReadRequest req, Long readerId) {
        Long roomId = req.roomId();
        Long newLastReadId = req.lastReadMessageId();

        ChatParticipant readerParticipant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, readerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        Long previousLastReadId = readerParticipant.getLastReadMessageId() == null ? 0L : readerParticipant.getLastReadMessageId();

        readerParticipant.setLastReadMessageId(newLastReadId);

        List<ChatParticipant> allParticipants = chatParticipantRepository.findByChatRoomId(roomId);

        List<ChatMessage> affectedMessages = chatMessageRepository
                .findByChatRoomIdAndIdGreaterThanAndIdLessThanEqualOrderByIdAsc(roomId, previousLastReadId, newLastReadId);

        List<ReadCountInfo> updatedReadCounts = new ArrayList<>();
        for (ChatMessage message : affectedMessages) {
            if (!message.getSender().getId().equals(readerId)) {
                int newUnreadCount = calculateUnreadCountForMessage(message, allParticipants);
                updatedReadCounts.add(new ReadCountInfo(message.getId(), newUnreadCount));
            }
        }

        if (!updatedReadCounts.isEmpty()) {
            messagingTemplate.convertAndSend(
                    "/topic/rooms/" + roomId + "/read-counts",
                    new MessageReadCountUpdateResponse(updatedReadCounts)
            );
        }
        ChatRoomSummaryResponse summary = buildChatRoomSummaryResponse(roomId, readerId);
        messagingTemplate.convertAndSend(
                "/topic/user/" + readerId + "/rooms",
                summary
        );

    }
    /**
     * 특정 사용자를 위한 ChatRoomSummaryResponse DTO를 생성합니다.
     * 채팅방 목록 UI에 사용될 데이터를 만듭니다.
     *
     * @param roomId    요약 정보를 생성할 채팅방의 ID
     * @param forUserId 요약 정보의 기준이 되는 사용자('나')의 ID
     * @return 생성된 ChatRoomSummaryResponse DTO
     */
    private ChatRoomSummaryResponse buildChatRoomSummaryResponse(Long roomId, Long forUserId) {
         ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatMessage lastMessage = chatMessageRepository.findTopByChatRoomIdOrderBySentAtDesc(roomId)
                .orElse(null);


        String lastMessageContent = (lastMessage != null) ? lastMessage.getContent() : "대화를 시작해보세요.";
        Instant lastMessageTime = (lastMessage != null) ? lastMessage.getSentAt() : room.getCreatedAt();
        int unreadCount = countUnreadMessages(roomId, forUserId);

        String roomName;
        String roomImageUrl;

        List<ChatParticipant> participants = room.getParticipants();
        int participantCount = participants.size();

        if (!room.getGroup()) {
            User opponent = participants.stream()
                    .map(ChatParticipant::getUser)
                    .filter(user -> !user.getId().equals(forUserId))
                    .findFirst()
                    .orElse(null);

            if (opponent == null) {
                roomName = "(알 수 없음)";
                roomImageUrl = null;
            } else {
                roomName = opponent. getFirstName() + " " + opponent.getLastName();
                roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, opponent.getId())
                        .map(Image::getUrl)
                        .orElse(null);
            }
        } else {
            roomName = room.getRoomName();
            roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.CHAT_ROOM, room.getId())
                    .map(Image::getUrl)
                    .orElse(null);
        }

        return new ChatRoomSummaryResponse(
                room.getId(),
                roomName,
                lastMessageContent,
                lastMessageTime,
                roomImageUrl,
                unreadCount,
                participantCount
        );
    }

    /**
     * 특정 메시지 하나를 몇 명의 참여자가 아직 읽지 않았는지 계산합니다.
     *
     * @param message         안 읽은 수를 계산할 대상 메시지
     * @param allParticipants 해당 채팅방의 모든 참여자 목록 (성능 최적화를 위해 미리 조회해서 전달)
     * @return 해당 메시지를 아직 읽지 않은 참여자의 수
     */
    private int calculateUnreadCountForMessage(ChatMessage message, List<ChatParticipant> allParticipants) {
        int totalParticipantCount = allParticipants.size();
        long readParticipantCount = allParticipants.stream()
                .filter(participant ->
                        participant.getLastReadMessageId() != null &&
                                participant.getLastReadMessageId() >= message.getId()
                )
                .count();
        return totalParticipantCount - (int) readParticipantCount;
    }

    public Instant getLastMessageTime(Long roomId) {
        return chatMessageRepository.findTopByChatRoomIdOrderBySentAtDesc(roomId)
                .map(ChatMessage::getSentAt)
                .orElse(null);
    }

    public ChatRoom getChatRoomById(Long roomId) {
        return chatRoomRepository.findByIdWithParticipantsAndUsers(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }
    @Transactional(readOnly = true)
    public GroupChatDetailResponse getGroupChatDetails(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<ChatParticipant> activeParticipants = chatRoom.getParticipants().stream()
                .filter(participant -> participant.getStatus() == ChatParticipantStatus.ACTIVE)
                .collect(Collectors.toList());

        String roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(
                ImageType.CHAT_ROOM, chatRoom.getId()
        ).map(Image::getUrl).orElse(null);

        Long ownerId = chatRoom.getOwner().getId();
        String ownerImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(
                ImageType.USER,
                ownerId
        ).map(Image::getUrl).orElse(null);

        List<String> otherParticipantsImageUrls = activeParticipants.stream()
                .filter(participant -> !participant.getUser().getId().equals(ownerId))
                .map(participant -> imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(
                        ImageType.USER,
                        participant.getUser().getId()
                ).map(Image::getUrl).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return GroupChatDetailResponse.from(
                chatRoom,
                roomImageUrl,
                activeParticipants.size(),
                otherParticipantsImageUrls,
                ownerImageUrl
        );
    }

    /**
     * 현재 사용자를 지정된 그룹 채팅방에 추가합니다.
     *
     * @param roomId 그룹 채팅방 ID
     * @param userId 참여를 요청하는 사용자의 ID
     */
    @Transactional
    public void joinGroupChat(Long roomId, Long userId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));


        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatRoom room = chatRoomRepo.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.getGroup()) {
            throw new BusinessException(ErrorCode.CHAT_NOT_GROUP);
        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .ifPresentOrElse(
                        participant -> {
                            if (participant.getStatus() == ChatParticipantStatus.ACTIVE) {
                                throw new BusinessException(ErrorCode.ALREADY_CHAT_PARTICIPANT);
                            } else {
                                participant.reJoin();
                            }
                        },
                        () -> {
                            ChatParticipant newParticipant = new ChatParticipant(room, user);
                            room.addParticipant(newParticipant);
                            chatParticipantRepository.save(newParticipant);
                        }
                );
    }

    /**
     * 그룹 채팅방을 이름 키워드로 검색합니다.
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 DTO 목록
     */
    public List<GroupChatSearchResponse> searchGroupChatRooms(String keyword) {
        List<ChatRoom> chatRooms = chatRoomRepository.findGroupChatRoomsByKeyword(keyword);

        return chatRooms.stream()
                .map(chatRoom -> {
                    String roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(
                            ImageType.CHAT_ROOM, chatRoom.getId()
                    ).map(Image::getUrl).orElse(null);

                    int participantCount = chatRoom.getParticipants().size();

                    return GroupChatSearchResponse.from(chatRoom, roomImageUrl, participantCount);
                })
                .collect(Collectors.toList());
    }

    public List<ChatRoomSummaryResponse> searchRoomsByRoomName(Long userId, String roomName) {
        List<ChatRoom> rooms = chatParticipantRepository.findChatRoomsByUserIdAndRoomName(userId, roomName);
        List<ChatRoomSummaryResponse> summaries = new ArrayList<>();
        for (ChatRoom room : rooms) {
            ChatRoomSummaryResponse summary = buildChatRoomSummaryResponse(room.getId(), userId);
            summaries.add(summary);
        }
        return summaries;
    }


    public int countUnreadMessages(Long roomId, Long userId) {
        Long lastReadId = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .map(ChatParticipant::getLastReadMessageId)
                .orElse(0L);

        return chatMessageRepository.countUnreadMessages(roomId, lastReadId, userId);
    }

    @Transactional
    public List<GroupChatMainResponse> getLatestGroupChats(Long lastChatRoomId) {
        List<ChatRoom> latestRooms;

        if (lastChatRoomId == null) {
            latestRooms = chatRoomRepository.findTop10ByGroupTrueOrderByCreatedAtDesc();
        } else {
            latestRooms = chatRoomRepository.findTop10ByGroupTrueAndIdLessThanOrderByCreatedAtDesc(lastChatRoomId);
        }

        return latestRooms.stream()
                .map(this::toGroupChatMainResponse)
                .collect(Collectors.toList());
    }

    private GroupChatMainResponse toGroupChatMainResponse(ChatRoom chatRoom) {
        String roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.CHAT_ROOM, chatRoom.getId())
                .map(Image::getUrl)
                .orElse(null);
        String userCount = String.valueOf(chatRoom.getParticipants().size());
        return new GroupChatMainResponse(
                chatRoom.getId(),
                chatRoom.getRoomName(),
                chatRoom.getDescription(),
                roomImageUrl,
                userCount
        );
    }

    @Transactional
    public List<GroupChatMainResponse> getPopularGroupChats(int limit) {
        List<ChatRoom> popularRooms = chatRoomRepository.findTopByGroupTrueOrderByParticipantCountDesc(limit);
        return popularRooms.stream()
                .map(this::toGroupChatSearchResponse)
                .collect(Collectors.toList());
    }

    private GroupChatMainResponse toGroupChatSearchResponse(ChatRoom chatRoom) {
        String roomImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(
                        ImageType.CHAT_ROOM, chatRoom.getId())
                .map(Image::getUrl)
                .orElse(null);
        String userCount = String.valueOf(chatRoom.getParticipants().size());
        return new GroupChatMainResponse(
                chatRoom.getId(),
                chatRoom.getRoomName(),
                chatRoom.getDescription(),
                roomImageUrl,
                userCount
        );
    }

    @Transactional
    public List<ChatMessageFirstResponse> getFirstMessages(Long roomId, Long userId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .filter(participant -> participant.getStatus() != ChatParticipantStatus.LEFT)
                .orElseThrow(() -> new IllegalArgumentException("채팅방에 참여하지 않았거나 나간 사용자입니다."));
        List<ChatMessage> messages = chatMessageRepository.findTop50ByChatRoomIdOrderBySentAtDesc(roomId);

        List<Long> blockedIds = getBlockedUserIds(userId);
        if (!blockedIds.isEmpty()) {
            messages = messages.stream()
                    .filter(msg -> !blockedIds.contains(msg.getSender().getId()))
                    .toList();
        }

        return messages.stream()
                .map(message -> ChatMessageFirstResponse.fromEntity(message, chatRoom, imageRepository))
                .collect(Collectors.toList());
    }

    /**
     * 사용자 프로필 조회 서비스 메서드
     *
     * @param userId 조회할 사용자의 ID
     * @return UserProfileResponse DTO
     */
    @Transactional(readOnly = true)
    public ChatUserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        Image image = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        return ChatUserProfileResponse.from(user, image.getUrl());
    }

    @Transactional
    public void toggleTranslation(Long roomId, Long userId, boolean enable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));
        participant.toggleTranslation(enable);
    }

    @Transactional
    public void processAndSendChatMessage(SendMessageRequest req) {
        Long startTime = System.currentTimeMillis();
        ChatMessage savedMessage = this.saveMessage(req.roomId(), req.senderId(), req.content());
        String originalContent = savedMessage.getContent();


        ChatRoom chatRoom = savedMessage.getChatRoom();
        List<ChatParticipant> participants = chatRoom.getParticipants();
        User senderUser = userRepository.findById(req.senderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String userImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, req.senderId())
                .map(Image::getUrl)
                .orElse(null);

        chatParticipantRepository.findByChatRoomIdAndUserId(req.roomId(), req.senderId())
                .ifPresent(participant -> {
                    participant.setLastReadMessageId(savedMessage.getId());
                    chatParticipantRepository.save(participant);
                });

        for (ChatParticipant participant : participants) {
            User recipient = participant.getUser();
            String targetContent = null;
            boolean isBlockedByRecipient = blockRepository.findBlockRelationship(recipient, senderUser).isPresent();
            boolean isBlockedByMe = blockRepository.findBlockRelationship(senderUser, recipient).isPresent();

            if (isBlockedByRecipient || isBlockedByMe) {
                continue;
            }


            if (participant.isTranslateEnabled()) {
                String targetLanguage = recipient.getTranslateLanguage();
                if (targetLanguage != null && !targetLanguage.isEmpty()) {
                    List<String> translatedList = translationService.translateMessages(List.of(originalContent), targetLanguage);
                    if (!translatedList.isEmpty()) {
                        targetContent = translatedList.get(0);
                    }
                }
            }
            if (!recipient.getId().equals(req.senderId())) {
                NotificationEvent event = new NotificationEvent(
                        recipient.getId(),
                        senderUser.getId(),
                        NotificationType.chat,
                        chatRoom.getId(),
                        originalContent
                );
                eventPublisher.publishEvent(event);
            }
            ChatMessageResponse messageResponse = new ChatMessageResponse(
                    savedMessage.getId(),
                    chatRoom.getId(),
                    savedMessage.getSender().getId(),
                    originalContent,
                    targetContent,
                    savedMessage.getSentAt(),
                    senderUser.getFirstName(),
                    senderUser.getLastName(),
                    userImageUrl,
                    MessageType.TEXT
            );
            String destination = String.format("/topic/user/%s/%s/messages",
                    recipient.getId(),
                    chatRoom.getId()
            );
            messagingTemplate.convertAndSend(destination, messageResponse);
            ChatRoomSummaryResponse summary = buildChatRoomSummaryResponse(chatRoom.getId(), recipient.getId());
            messagingTemplate.convertAndSend("/topic/user/" + recipient.getId() + "/rooms", summary);
            long endTime = System.currentTimeMillis();
            log.info("Processed MEDIA message for roomId={} in {}ms", req.roomId(), (endTime - startTime));
        }
    }

    @Transactional
    public void markAllMessagesAsReadInRoom(Long roomId, Long readerId) {
        Optional<ChatMessage> lastMessageOpt = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId);

        if (lastMessageOpt.isPresent()) {
            ChatMessage lastMessage = lastMessageOpt.get();
            Long lastMessageId = lastMessage.getId();
            AllmarkMessagesAsRead(roomId, readerId, lastMessageId);

            // 문제의 그 로그 남겨둠
            log.info(">>>> All messages marked as read for userId: {} in roomId: {}", readerId, roomId);
        } else {
            log.info(">>>> No messages to mark as read in roomId: {}", roomId);
        }
    }

    /**
     * 특정 메시지 ID까지 읽음 처리하는 기존 메서드 (이전 답변의 효율적인 버전)
     */
    @Transactional
    public void AllmarkMessagesAsRead(Long roomId, Long readerId, Long lastReadMessageId) {
        ChatParticipant readerParticipant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, readerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND));

        readerParticipant.setLastReadMessageId(lastReadMessageId);
    }

    /**
     * 새로운 그룹 채팅방을 생성합니다.
     * @param userId 생성 요청을 한 사용자(소유자)의 ID
     * @param request 채팅방 생성에 필요한 정보 DTO
     */
    @Transactional
    public void createGroupChatRoom(Long userId, CreateGroupChatRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(owner.getBirthdate()==null||owner.getPurpose()==null||owner.getIntroduction()==null||owner.getLanguage()==null||owner.getHobby()==null||owner.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        if (request.roomName() == null || request.roomName().isBlank()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        ChatRoom newRoom = new ChatRoom(
                true,
                Instant.now(),
                request.roomName().trim(),
                request.description(),
                owner
        );
        ChatRoom savedRoom = chatRoomRepository.save(newRoom);

        ChatParticipant ownerParticipant = new ChatParticipant(savedRoom, owner);
        chatParticipantRepository.save(ownerParticipant);

        if (request.roomImageUrl() != null && !request.roomImageUrl().isBlank()) {
            try {
                imageService.upsertChatRoomProfileImage(savedRoom.getId(), request.roomImageUrl());
            } catch (Exception e) {
                log.error("채팅방 이미지 저장/업데이트에 실패했습니다. Room ID: {}", savedRoom.getId(), e);
                throw new BusinessException(ErrorCode.IMAGE_PROCESSING_FAILED);
            }
        }
    }

    /**
     * @apiNote 특정 메시지를 중심으로 이전/이후 메시지를 함께 조회합니다. (리팩토링 미적용 버전)
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesAround(Long roomId, Long userId, Long targetMessageId) {
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

        List<ChatMessage> olderMessages = chatMessageRepository.findTop20ByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, targetMessageId);
        Collections.reverse(olderMessages);

        ChatMessage targetMessage = chatMessageRepository.findById(targetMessageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        List<ChatMessage> newerMessages = chatMessageRepository.findTop20ByChatRoomIdAndIdGreaterThanOrderByIdAsc(roomId, targetMessageId);

        List<ChatMessage> combinedMessages = new ArrayList<>();
        combinedMessages.addAll(olderMessages);
        combinedMessages.add(targetMessage);
        combinedMessages.addAll(newerMessages);

        boolean needsTranslation = participant.isTranslateEnabled();
        String targetLanguage = participant.getUser().getTranslateLanguage();

        if (needsTranslation && targetLanguage != null && !targetLanguage.isEmpty()) {
            List<String> originalContents = combinedMessages.stream()
                    .map(ChatMessage::getContent)
                    .collect(Collectors.toList());
            List<String> translatedContents = translationService.translateMessages(originalContents, targetLanguage);

            return IntStream.range(0, combinedMessages.size())
                    .mapToObj(i -> {
                        ChatMessage message = combinedMessages.get(i);
                        String translatedContent = translatedContents.get(i);
                        User sender = message.getSender();
                        String senderImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                                .map(Image::getUrl)
                                .orElse(null);

                        return new ChatMessageResponse(
                                message.getId(),
                                message.getChatRoom().getId(),
                                sender.getId(),
                                message.getContent(),
                                translatedContent,
                                message.getSentAt(),
                                sender.getFirstName(),
                                sender.getLastName(),
                                senderImageUrl,
                                message.getMessageType()
                        );
                    }).collect(Collectors.toList());
        } else {
            return combinedMessages.stream()
                    .map(message -> {
                        User sender = message.getSender();
                        String senderImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                                .map(Image::getUrl)
                                .orElse(null);

                        return new ChatMessageResponse(
                                message.getId(),
                                message.getChatRoom().getId(),
                                sender.getId(),
                                message.getContent(),
                                null,
                                message.getSentAt(),
                                sender.getFirstName(),
                                sender.getLastName(),
                                senderImageUrl,
                                message.getMessageType()
                        );
                    }).collect(Collectors.toList());
        }
    }

    /**
     * @apiNote 메시지를 DB에서 삭제하고, 해당 채팅방에 삭제 이벤트를 브로드캐스팅합니다.
     */
    @Transactional
    public void deleteMessageAndBroadcast(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getSender().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_MESSAGE_DELETE);
        }
        Map<String, String> payload = Map.of(
                "id", messageId.toString(),
                "type", "delete"
        );

        chatMessageRepository.delete(message);
        String destination = "/topic/rooms/" + message.getChatRoom().getId();
        messagingTemplate.convertAndSend(destination, payload);
    }

    private List<Long> getBlockedUserIds(Long userId) {
        return blockRepository.findByUserId(userId).stream()
                .map(BlockUser::getBlocked)
                .map(User::getId)
                .toList();
    }

    /**
     * roomId에 해당하는 채팅방이 그룹인지 확인
     *
     * @param roomId 채팅방 ID
     * @return 그룹이면 true, 1:1이면 false
     */
    public boolean isChatRoomGroup(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return Boolean.TRUE.equals(room.getGroup());
    }

    @Transactional
    public void blockChatUser(Long userId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User blockedUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(user.getBirthdate()==null||user.getPurpose()==null||user.getIntroduction()==null||user.getLanguage()==null||user.getHobby()==null||user.getSex()==null){
            throw new BusinessException(ErrorCode.PROFILE_SET_NOT_COMPLETED);
        }

        if (blockedUser.getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK);
        }

        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (blockRepository.existsBlock(me.getId(), blockedUser.getId()) || blockRepository.existsBlock(blockedUser.getId(), me.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK);
        }

        blockRepository.save(new BlockUser(me, blockedUser));
    }

    private record ChatRoomWithTime(ChatRoom room, Instant lastMessageTime) {
    }

    private record MessagePair(ChatMessage originalMessage, String translatedContent) {
    }

    @Transactional
    public void processAndSendMediaMessage(SendMediaMessageRequest req) {
        ChatRoom chatRoom = chatRoomRepository.findById(req.roomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        User sender = userRepository.findById(req.senderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatMessage savedMessage = new ChatMessage(chatRoom, sender, req.mediaKey(), req.messageType());
        chatMessageRepository.save(savedMessage);

        chatParticipantRepository.findByChatRoomIdAndUserId(req.roomId(), req.senderId())
                .ifPresent(participant -> participant.setLastReadMessageId(savedMessage.getId()));

        String senderImageUrl = imageRepository.findFirstByImageTypeAndRelatedIdOrderByOrderIndexAsc(ImageType.USER, sender.getId())
                .map(Image::getUrl)
                .orElse(null);

        for (ChatParticipant participant : chatRoom.getParticipants()) {
            User recipient = participant.getUser();

            boolean isBlocked = blockRepository.findBlockRelationship(recipient, sender).isPresent() ||
                    blockRepository.findBlockRelationship(sender, recipient).isPresent();
            if (isBlocked) {
                continue;
            }
            if (!recipient.getId().equals(sender.getId())) {
                String contentSnippet = req.messageType() == MessageType.IMAGE ? "사진을 보냈습니다." : "동영상을 보냈습니다.";
                NotificationEvent event = new NotificationEvent(
                        recipient.getId(),
                        sender.getId(),
                        NotificationType.chat,
                        chatRoom.getId(),
                        contentSnippet
                );
                eventPublisher.publishEvent(event);
            }
            String fullMediaUrl = cdnBaseUrl + "/" + savedMessage.getContent();

            ChatMessageResponse messageResponse = new ChatMessageResponse(
                    savedMessage.getId(),
                    chatRoom.getId(),
                    sender.getId(),
                    fullMediaUrl,
                    null,
                    savedMessage.getSentAt(),
                    sender.getFirstName(),
                    sender.getLastName(),
                    senderImageUrl,
                    savedMessage.getMessageType()
            );

            String destination = String.format("/topic/user/%s/%s/messages",
                    recipient.getId(),
                    chatRoom.getId()
            );
            messagingTemplate.convertAndSend(destination, messageResponse);
            ChatRoomSummaryResponse summary = buildChatRoomSummaryResponse(chatRoom.getId(), recipient.getId());
            messagingTemplate.convertAndSend("/topic/user/" + recipient.getId() + "/rooms", summary);
        }
    }





    /**
     * 채팅 미디어 전용 Presigned URL을 생성합니다. (AWS SDK v2 방식)
     * @param chatroomId 파일이 속할 채팅방 ID
     * @param fileName 클라이언트가 전송한 원본 파일 이름
     * @return PresignedUrl과 S3에 저장될 최종 파일 키(Key)
     */
    public PresignedUrlResponse generateChatPresignedUrl(Long chatroomId, String fileName) {
        String fileKey = "chats/" + chatroomId + "/" + UUID.randomUUID() + "-" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedPutObjectRequest.url().toString();

        return new PresignedUrlResponse(presignedUrl, fileKey);
    }

    /**
     * 특정 채팅방에 대한 사용자의 알림 설정을 변경합니다.
     *
     * @param roomId  설정을 변경할 채팅방 ID
     * @param userId  설정을 변경하는 사용자 ID
     * @param enabled 알림을 활성화할지 여부
     */
    @Transactional
    public void toggleChatRoomNotifications(Long roomId, Long userId, boolean enabled) {

        Optional<ChatParticipant> participantOptional = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId);
        if (participantOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.CHAT_PARTICIPANT_NOT_FOUND);
        }

        ChatParticipant participant = participantOptional.get();
        participant.setNotificationsEnabled(enabled);

    }
}
