package core.domain.chat.controller;
import core.domain.chat.dto.*;
import core.domain.chat.service.ChatAiService;
import core.global.config.CustomUserDetails;
import core.global.dto.ApiResponse;
import core.global.metrics.FeatureUsageMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "AI 채팅", description = "AI와의 채팅방 생성, 메시지 전송, 내역 조회, 삭제")
@RestController
@RequestMapping("/api/v1/chat/ai")
@RequiredArgsConstructor
public class ChatAiController {

    private final ChatAiService chatAiService;
    private final FeatureUsageMetrics featureUsageMetrics;


    @Operation(summary = "AI와 새로운 채팅방 생성", description = "AI와 1:1 채팅방을 생성합니다. 기존 방이 있으면 isNew: false, 새로 생성되면 isNew: true를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 생성/조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatAiRoomResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PostMapping("/rooms")
    
    public ResponseEntity<ApiResponse<ChatAiRoomResponse>> createAiRoom() {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ChatAiRoomResponse response = chatAiService.createAiChatRoom(principal.getUserId());
        featureUsageMetrics.recordChatUsage();

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "AI 채팅방에 메시지 보내기", description = "지정된 AI 채팅방에 메시지를 보내고 AI의 답변을 받습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 전송 및 AI 응답 수신 성공",
                    content = @Content(schema = @Schema(implementation = AiMessageResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 채팅방에 접근 권한이 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방", content = @Content)
    })
    @PostMapping("/rooms/{roomId}/messages")
    
    public ResponseEntity<ApiResponse<AiMessageResponse>> sendMessage(
            @PathVariable Long roomId,
            @RequestBody AiMessageRequest request
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AiMessageResponse response = chatAiService.sendMessageToAi(principal.getUserId(), roomId, request);
        featureUsageMetrics.recordChatUsage();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "AI 채팅방 삭제", description = "AI와의 채팅방 및 모든 대화 기록을 영구적으로 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 및 데이터 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 채팅방에 접근 권한이 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방", content = @Content)
    })
    @DeleteMapping("/rooms/{roomId}")
    
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
                                                        @PathVariable Long roomId
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatAiService.deleteAiChatRoom(principal.getUserId(), roomId);
        featureUsageMetrics.recordChatUsage();

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "AI 채팅방 메시지 불러오기 (무한 스크롤)", description = "특정 AI 채팅방의 대화 기록을 20개씩 불러옵니다. 최초 요청 시에는 lastMessageId를 보내지 마세요.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = MessageSliceResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 채팅방에 접근 권한이 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방", content = @Content)
    })
    @GetMapping("/rooms/{roomId}/messages")
    
    public ResponseEntity<ApiResponse<MessageSliceResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MessageSliceResponse messages = chatAiService.getChatMessages(principal.getUserId(), roomId, lastMessageId);
        featureUsageMetrics.recordChatUsage();

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @Operation(summary = "AI 메시지 신고 및 삭제", description = "AI가 생성한 부적절한 메시지를 신고하고 채팅방에서 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 신고 및 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "메시지를 삭제할 권한이 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 메시지")
    })
    @DeleteMapping("/messages/{messageId}")
    
    public ResponseEntity<ApiResponse<Void>> reportAndDeleteMessage(
            @PathVariable Long messageId
    ) {
        CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatAiService.deleteReportedMessage(principal.getUserId(), messageId);
        featureUsageMetrics.recordChatUsage();

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}