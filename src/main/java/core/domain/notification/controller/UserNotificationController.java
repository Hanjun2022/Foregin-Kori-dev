package core.domain.notification.controller;

import core.domain.notification.dto.*;
import core.domain.notification.service.UserNotificationService;
import core.domain.usernotificationsetting.dto.NotificationSettingBulkUpdateRequestDto;
import core.domain.usernotificationsetting.dto.NotificationSettingInitRequestDto;
import core.domain.usernotificationsetting.dto.NotificationSettingResponseDto;
import core.domain.usernotificationsetting.dto.NotificationSettingBulkUpdateRequestDto;
import core.domain.usernotificationsetting.service.UserNotificationSettingService;
import core.global.config.CustomUserDetails;
import core.global.dto.ApiResponse;
import core.global.enums.NotificationType;
import core.global.image.dto.NotificationSliceResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "사용자 알림 및 설정", description = "사용자 기기 토큰 및 알림 설정 관련 API")
@RestController
@RequestMapping("/api/v1/user/notification")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @Operation(summary = "FCM 기기 토큰 등록/갱신", description = "클라이언트의 FCM 기기 토큰을 서버에 등록하거나 최신으로 업데이트합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @PostMapping("/device-token")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody DeviceTokenRequest request
    ) {
        userNotificationService.registerDeviceToken(userDetails.getUserId(), request.deviceToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "알림 설정 상태 확인 (마이그레이션용)", description = "사용자가 새로운 알림 설정 절차를 완료했는지 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = NotificationSettingStatusResponse.class)))
    })
    @GetMapping("/settings-status")
    public ResponseEntity<ApiResponse<NotificationSettingStatusResponse>> getNotificationSettingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NotificationSettingStatusResponse response = userNotificationService.getNotificationSettingStatus(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "알림 설정 초기화", description = "신규/기존 사용자의 앱 내부 알림 동의 선택 결과를 서버에 저장합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> initializeNotificationSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody NotificationSettingInitRequest request
    ) {
        userNotificationService.initializeNotificationSettings(userDetails.getUserId(), request.agreed());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "OS 푸시 권한 상태 동기화", description = "사용자의 실제 OS 푸시 권한 상태를 서버와 동기화합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    })
    @PutMapping("/push-agreement")
    public ResponseEntity<ApiResponse<Void>> syncPushAgreement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PushAgreementRequest request
    ) {
        userNotificationService.syncPushAgreement(userDetails.getUserId(), request.osPermissionGranted());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private final UserNotificationSettingService notificationSettingService;

    @Operation(
            summary = "사용자 알림 설정 조회",
            description = "현재 로그인한 사용자의 알림 설정 목록을 조회합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotificationSettingsExample",
                                            value = """
                                                    {
                                                      "message": "success",
                                                      "data": [
                                                    { "notificationType": "post", "enabled": true },
                                                    { "notificationType": "comment", "enabled": true },
                                                    { "notificationType": "chat", "enabled": false },
                                                    { "notificationType": "follow", "enabled": true },
                                                    { "notificationType": "receive", "enabled": false },
                                                    { "notificationType": "followuserpost", "enabled": true },
                                                    { "notificationType": "newuser", "enabled": true },
                                                      ],
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                   \s"""
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "UnauthorizedExample",
                                            value = """
                                                    {
                                                      "message": "로그인이 필요합니다.",
                                                      "data": null,
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "알림 설정을 찾을 수 없음",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotFoundExample",
                                            value = """
                                                    {
                                                      "message": "알림 설정을 찾을 수 없습니다.",
                                                      "data": null,
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationSettingResponseDto>>> getNotificationSettings( @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        System.out.println("getNotificationSettings called! userId=" + userId);
        return ResponseEntity.ok(ApiResponse.success(notificationSettingService.getUserNotificationSettings(userId)));
    }

    @Operation(
            summary = "사용자 알림 설정 수정",
            description = "현재 로그인한 사용자의 특정 알림 설정을 변경합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 알림 설정 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = NotificationSettingBulkUpdateRequestDto.class),
                            examples = @ExampleObject(
                                    name = "NotificationSettingBulkUpdateRequestDtoExample",
                                    value = """
                                            {
                                              "settings": [
                                                { "notificationType": "post", "enabled": true },
                                                { "notificationType": "comment", "enabled": false },
                                                { "notificationType": "chat", "enabled": true },
                                                { "notificationType": "follow", "enabled": true },
                                                { "notificationType": "receive", "enabled": false },
                                                { "notificationType": "followuserpost", "enabled": true },
                                                { "notificationType": "newuser", "enabled": true }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "변경 성공",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotificationSettingUpdateResponseExample",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "data": {
                                                        "settings": [
                                                          { "type": "post", "enabled": true },
                                                          { "type": "comment", "enabled": false },
                                                          { "type": "chat", "enabled": true },
                                                          { "type": "follow", "enabled": true },
                                                          { "type": "receive", "enabled": false }
                                                          { "notificationType": "followuserpost", "enabled": true },
                                                          { "notificationType": "newuser", "enabled": true }
                                                 
                                                        ]
                                                      }
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "UnauthorizedExample",
                                            value = """
                                                    {
                                                      "message": "로그인이 필요합니다.",
                                                      "data": null,
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "알림 설정을 찾을 수 없음",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotFoundExample",
                                            value = """
                                                    {
                                                      "message": "알림 설정을 찾을 수 없습니다.",
                                                      "data": null,
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @PutMapping
    public ResponseEntity<ApiResponse<NotificationSettingListResponse>> updateNotificationSetting(
            @RequestBody NotificationSettingBulkUpdateRequestDto request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.success(notificationSettingService.updateUserNotificationSettings(userId, request)));
    }

    @Operation(
            summary = "사용자 알림 설정 초기화",
            description = "유저가 알림 설정을 초기값으로 설정합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "초기 알림 설정 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = NotificationSettingInitRequestDto.class),
                            examples = @ExampleObject(
                                    name = "NotificationInitExample",
                                    value = """
                                            {
                                              "settings": [
                                                { "notificationType": "post", "enabled": true },
                                                { "notificationType": "comment", "enabled": true },
                                                { "notificationType": "chat", "enabled": true },
                                                { "notificationType": "follow", "enabled": true },
                                                { "notificationType": "receive", "enabled": true },
                                                { "notificationType": "followuserpost", "enabled": true },
                                                { "notificationType": "newuser", "enabled": true }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "초기화 성공",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotificationInitResponseExample",
                                            value = """
                                                    {
                                                      "message": "success",
                                                      "data": null,
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "UnauthorizedExample",
                                            value = """
                                                    {
                                                      "message": "로그인이 필요합니다.",
                                                      "data": null,
                                                      "timestamp": "2025-10-03T12:00:00"git
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "사용자 없음",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "UserNotFoundExample",
                                            value = """
                                                    {
                                                      "message": "존재하지 않는 유저입니다.",
                                                      "data": null,
                                                      "timestamp": "2025-10-03T12:00:00"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<Void>> initializeSettings(
            @RequestBody NotificationSettingInitRequestDto request,@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        notificationSettingService.initializeNotificationSettings(userId, request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 '읽음' 상태로 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없거나, 본인의 알림이 아닐 경우")
    })
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        userNotificationService.markNotificationAsRead(userDetails.getUserId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "알림 목록 조회", description = "최근 7일간의 알림 목록을 조회합니다. 타입으로 필터링할 수 있습니다.")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<NotificationSliceResponseDto>> getNotifications(
                                                                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                                       @RequestParam(required = false) NotificationType type,
                                                                                       @PageableDefault(size = 20) Pageable pageable
    ) {
        NotificationSliceResponseDto response = userNotificationService.getNotifications(
                userDetails.getUserId(),
                type,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}