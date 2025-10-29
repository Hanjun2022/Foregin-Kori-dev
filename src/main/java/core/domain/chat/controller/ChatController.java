package core.domain.chat.controller;

import core.domain.chat.dto.*;
import core.domain.chat.entity.ChatRoom;
import core.domain.chat.service.ChatService;
import core.global.config.CustomUserDetails;
import core.global.dto.ApiResponse;

import core.global.metrics.FeatureUsageMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "채팅 API", description = "1:1 채팅, 그룹 채팅, 메시지 검색/삭제 등 채팅 기능 API")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final FeatureUsageMetrics featureUsageMetrics;

    private final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Operation(summary = "1:1 새로운 채팅방 생성", description = "1:1 채팅방을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatRoomResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })
    @PostMapping("/rooms/oneTone")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
            @RequestBody CreateRoomRequest request
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();
        ChatRoom room = chatService.createRoom(userId, request.otherUserId());
        ChatRoomResponse response = ChatRoomResponse.from(room);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "자신의 채팅방 리스트 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatRoomResponse.class))
            )
    })
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> ChatRooms() {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();
        List<ChatRoomSummaryResponse> responses = chatService.getMyAllChatRoomSummaries(userId);
        ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> responseEntity =
                ResponseEntity.ok(ApiResponse.success(responses));
        log.info(">>>> Returning ChatRooms Response: {}", responseEntity);
        return responseEntity;
    }
    @Operation(summary = "채팅방 나가기")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 유저",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })
    @DeleteMapping("/rooms/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(@PathVariable Long roomId) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();
        chatService.leaveRoom(roomId, userId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(null));
    }


    @Operation(summary = "채팅방 메시지 조회 (무한 스크롤 위로 스크롤올릴때 호출하는 api )")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 유저",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId
    ) {
        log.info("lastMessageId: 이거입니다!: {}",lastMessageId);
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();

        List<ChatMessageResponse> responses = chatService.getMessages(roomId, userId, lastMessageId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    @Operation(summary = "첫 채팅방 메시지 조회", description = "채팅방에 처음 입장 시 가장 최근 메시지 50개를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageFirstResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방 또는 유저를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })
    @GetMapping("/rooms/{roomId}/first_messages")
    public ResponseEntity<ApiResponse<List<ChatMessageFirstResponse>>> getFirstMessages(
            @PathVariable Long roomId
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();
        List<ChatMessageFirstResponse> responses = chatService.getFirstMessages(roomId, userId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "채팅 참여자 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatRoomParticipantsResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })
    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<ApiResponse<List<ChatRoomParticipantsResponse>>> getParticipants(@PathVariable Long roomId) {
        List<ChatRoomParticipantsResponse> responses = chatService.getRoomParticipants(roomId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }



    @Operation(summary = "그룹 채팅 참여")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 참여 중인 채팅방이거나, 그룹 채팅방이 아닐 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 유저")
    })
    @PostMapping("/rooms/group/{roomId}/join")
    public ResponseEntity<ApiResponse<Void>> joinGroupChat(@PathVariable Long roomId) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();

        chatService.joinGroupChat(roomId, userId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "메시지 키워드 검색", description = "메시지 내용을 키워드로 검색합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = Object.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 유저",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })
    @GetMapping("/search")
    
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> searchMessages(
            @RequestParam Long roomId,
            @RequestParam String keyword
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();

        List<ChatMessageResponse> responses = chatService.searchMessages(roomId, userId, keyword);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    @Operation(summary = "특정 메시지 주변의 채팅 내용 조회", description = "검색 등에서 특정 메시지로 바로 이동할 때 사용합니다. 해당 메시지 기준 이전 20개, 이후 20개의 메시지를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = Object.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 메세지",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })
    @GetMapping("/rooms/{roomId}/messages/around")
    
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessagesAround(
            @PathVariable Long roomId,
            @RequestParam Long messageId
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();

        List<ChatMessageResponse> responses = chatService.getMessagesAround(roomId, userId, messageId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }


    @Operation(summary = "그룹 채팅 상세 정보 조회", description = "그룹 채팅방의 상세 정보(이름, 오너, 참여자 목록 등)를 조회합니다.")
    @GetMapping("/rooms/group/{roomId}")
    
    public ResponseEntity<ApiResponse<GroupChatDetailResponse>> getGroupChatDetails(
            @PathVariable Long roomId) {
        GroupChatDetailResponse response = chatService.getGroupChatDetails(roomId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @Operation(summary = "그룹 채팅방 검색", description = "채팅방 이름 키워드를 통해 그룹 채팅방을 검색합니다.")
    @GetMapping("/rooms/group/search")
    
    public ResponseEntity<ApiResponse<List<GroupChatSearchResponse>>> searchGroupChats(@RequestParam String keyword) {
        List<GroupChatSearchResponse> response = chatService.searchGroupChatRooms(keyword);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채팅방 이름 검색", description = "사용자가 참여 중인 채팅방을 이름 키워드로 검색합니다. 1:1, 그룹 채팅 모두 포함됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatRoomSummaryResponse.class))
            ),
    })
    @GetMapping("/rooms/search")
    
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> searchRooms(
            @RequestParam("roomName") String roomName
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ChatRoomSummaryResponse> responses = chatService.searchRoomsByRoomName(principal.getUserId(), roomName);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "최신 그룹 채팅방 10개 조회", description = "가장 최근에 생성된 그룹 채팅방 10개를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = GroupChatSearchResponse.class))
            )
    })
    @GetMapping("/group/latest")
    
    public ResponseEntity<ApiResponse<List<GroupChatMainResponse>>> getLatestGroupChats(
            @RequestParam(required = false) Long lastChatRoomId) {
        List<GroupChatMainResponse> response = chatService.getLatestGroupChats(lastChatRoomId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "인기 그룹 채팅방 10개 조회", description = "참여자가 가장 많은 그룹 채팅방 10개를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = GroupChatSearchResponse.class))
            )
    })
    @GetMapping("/group/popular")
    
    public ResponseEntity<ApiResponse<List<GroupChatMainResponse>>> getPopularGroupChats() {
        List<GroupChatMainResponse> response = chatService.getPopularGroupChats(10);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @Operation(summary = "유저 프로필 조회", description = "userId를 통해 유저의 상세 프로필 정보와 이미지 URL을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ChatUserProfileResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 유저",
                    content = @Content(schema = @Schema(implementation = Object.class))
            )
    })

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<ApiResponse<ChatUserProfileResponse>> getUserProfile(@PathVariable Long userId) {
        ChatUserProfileResponse response = chatService.getUserProfile(userId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채팅방 번역 기능 설정", description = "특정 채팅방의 메시지 번역 기능을 켜거나 끕니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방 또는 참여자를 찾을 수 없음")
    })
    @PostMapping("/rooms/{roomId}/translation")
    
    public ResponseEntity<ApiResponse<Void>> toggleTranslation(
            @PathVariable Long roomId,
            @RequestBody ToggleTranslationRequest request
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();

        chatService.toggleTranslation(roomId, userId, request.translateEnabled());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "채팅방의 모든 메시지를 읽음 처리", description = "해당 채팅방(roomId)의 모든 메시지를 현재 사용자 기준으로 읽음 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @PostMapping("/rooms/{roomId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long roomId) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();
        chatService.markAllMessagesAsReadInRoom(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @Operation(summary = "그룹 채팅방 생성", description = "새로운 그룹 채팅방을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/rooms/group")
    public ResponseEntity<ApiResponse<Void>> createGroupChat(
                                                              @Valid @RequestBody CreateGroupChatRequest request
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();
        chatService.createGroupChatRoom(userId, request);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }
    @Operation(summary = "신고 더미 API", description = "신고 요청 수락용 더미 엔드포인트입니다. 실제 동작은 하지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK 응답 반환")
    })
    @PostMapping("/declaration")
    public ResponseEntity<ApiResponse<Void>> okOnly(@RequestBody String ignored) {
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @Operation(summary = "채팅방이 그룹인지 여부 확인", description = "roomId에 해당하는 채팅방이 그룹 채팅방인지(1:1 채팅인지) 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = ChatRoomGroupResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @GetMapping("/isGroup")
    public ResponseEntity<ApiResponse<ChatRoomGroupResponse>> isChatRoomGroup(
            @RequestParam Long roomId) {
        boolean isGroup = chatService.isChatRoomGroup(roomId);
        ChatRoomGroupResponse response = new ChatRoomGroupResponse(isGroup);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @Operation(summary = "특정 사용자 차단", description = "대화 상대를 차단합니다. 이미 차단되어 있거나 자기 자신은 차단할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "차단 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 차단됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자를 찾을 수 없음")
    })
    @PostMapping("/block/{targetUserId}")
    public ResponseEntity<core.global.dto.ApiResponse<?>> blockUser(
            @PathVariable @Positive Long targetUserId
    ) {
        chatService.blockChatUser(targetUserId);
        featureUsageMetrics.recordChatUsage();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(core.global.dto.ApiResponse.success("차단 성공"));
    }
    @Operation(summary = "채팅 미디어 Presigned URL 발급", // ✅ API 제목
            description = """
               지정된 채팅방(chatroomId)에 사진이나 동영상을 업로드할 수 있는, 15분간 유효한 일회성 URL을 발급합니다.
               
               클라이언트는 이 응답으로 받은 `presignedUrl`에 `PUT` 메서드를 사용하여 바이너리 파일 데이터를 직접 업로드해야 합니다.
               
               업로드 성공 후에는 응답으로 받은 `fileKey` 값을 사용하여 WebSocket으로 최종 메시지를 전송해야 합니다.
               """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 ID일 경우")
    })
    @PostMapping("/presigned-url/chat/{chatroomId}")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getChatPresignedUrl(
            @PathVariable Long chatroomId,
            @RequestBody PresignedUrlRequest request) {

        PresignedUrlResponse response = chatService.generateChatPresignedUrl(chatroomId, request.fileName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @Operation(summary = "채팅방 알림 설정 변경", description = "특정 채팅방의 알림을 켜거나 끕니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방 또는 해당 채팅방의 참여자가 아닐 경우")
    })
    @PostMapping("/rooms/{roomId}/notifications")
    public ResponseEntity<ApiResponse<Void>> toggleChatRoomNotifications(
            @Parameter(description = "설정을 변경할 채팅방의 ID") @PathVariable Long roomId,
            @Valid @RequestBody ToggleNotificationsRequest request
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal.getUserId();

        chatService.toggleChatRoomNotifications(roomId, userId, request.enabled());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

}