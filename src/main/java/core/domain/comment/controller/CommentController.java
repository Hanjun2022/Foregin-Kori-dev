package core.domain.comment.controller;

import core.domain.comment.dto.CommentItem;
import core.domain.comment.dto.CommentUpdateRequest;
import core.domain.comment.dto.CommentWriteRequest;
import core.domain.comment.dto.UserCommentItem;
import core.domain.comment.service.CommentService;
import core.global.enums.SortOption;
import core.global.metrics.FeatureUsageMetrics;
import core.global.pagination.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Comments", description = "댓글 조회/작성/수정/삭제 API")
public class CommentController {
    private final CommentService commentService;
    private final FeatureUsageMetrics featureUsageMetrics;


    public CommentController(CommentService commentService, FeatureUsageMetrics featureUsageMetrics) {
        this.commentService = commentService;
        this.featureUsageMetrics = featureUsageMetrics;
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = """
            - 커서 기반 무한스크롤: 응답의 `nextCursor`를 다음 호출의 `cursor`로 그대로 전달하세요.
            - 정렬: LATEST(기본) | POPULAR
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "게시글 없음", content = @Content)
    })
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<core.global.dto.ApiResponse<CursorPageResponse<CommentItem>>> getCommentList(
            @Parameter(description = "게시글 ID", example = "123") @PathVariable("postId") Long postId,
            @Parameter(description = "페이지 크기(1~100)", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "정렬 옵션", example = "LATEST") @RequestParam(defaultValue = "LATEST") SortOption sort,
            @Parameter(description = "다음 페이지 호출 시 전달하는 불투명 커서(Base64). 첫 페이지는 생략")
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "false") Boolean translate
    ) {
        return ResponseEntity.ok(
                core.global.dto.ApiResponse.success(
                        commentService.getCommentList(postId, size, sort, cursor, translate)
                )
        );
    }


    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 오류", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<core.global.dto.ApiResponse<?>> writeComment(
            @Parameter(description = "게시글 ID", example = "123") @PathVariable("postId") Long postId,
            @Valid @RequestBody CommentWriteRequest request
    ) {
        commentService.writeComment(postId, request);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(core.global.dto.ApiResponse.success("댓글 작성 완료"));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "수정 성공(본문 없음)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 오류", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음", content = @Content)
    })
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<core.global.dto.ApiResponse<?>> updateComment(
            @Parameter(description = "댓글 ID", example = "98765") @PathVariable("commentId") Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        commentService.updateComment(commentId, request);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(core.global.dto.ApiResponse.success("댓글 수정 완료"));
    }


    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공(본문 없음)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음", content = @Content)
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<core.global.dto.ApiResponse<?>> deleteComment(
            @Parameter(description = "댓글 ID", example = "98765") @PathVariable("commentId") Long commentId
    ) {
        commentService.deleteComment( commentId);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(core.global.dto.ApiResponse.success("댓글 삭제 완료"));
    }

    @Operation(summary = "좋아요 추가", description = "댓글 좋아요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "추가 성공(본문 없음)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음", content = @Content)
    })
    @PutMapping("/comments/{commentId}/likes/me")
    public ResponseEntity<core.global.dto.ApiResponse<?>> addLike(
            @Parameter(description = "댓글 ID", example = "98765") @PathVariable("commentId") Long commentId
    ) {
        commentService.addLike( commentId);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(core.global.dto.ApiResponse.success("댓글 좋아요 완료"));
    }

    @Operation(summary = "좋아요 삭제", description = "댓글의 좋아요를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공(본문 없음)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음", content = @Content)
    })
    @DeleteMapping("/comments/{commentId}/likes/me")
    public ResponseEntity<core.global.dto.ApiResponse<?>> deleteLike(
            @Parameter(description = "댓글 ID", example = "98765") @PathVariable("commentId") Long commentId
    ) {
        commentService.deleteLike( commentId);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(core.global.dto.ApiResponse.success("댓글 좋아요 삭제 완료"));
    }


    @Operation(
            summary = "나의 댓글 목록 조회",
            description = """
          - 무한스크롤: 응답의 `nextCursor`를 다음 호출의 `cursor`로 그대로 전달
          
          요청 예시
          첫 페이지:
            GET /api/v1/boards/posts/123/comments?size=20
          다음 페이지:
            GET /api/v1/boards/posts/123/comments?size=20&cursor=eyJ0IjoiMjAyNS0wOC0wMVQxMjozNDo1NloiLCJpZCI6OTg3NjV9
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "success": true,
                          "data": {
                            "items": [
                              { "commentId": 321, "postContent": "원글 일부...", "commentContent": "댓글...", "createdAt": "2025-08-20T12:00:00Z" }
                            ],
                            "hasNext": true,
                            "nextCursor": "eyJpZCI6MzE5fQ"
                          }
                        }
                        """
                    )
            )),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @GetMapping("/my/comments")
    public ResponseEntity<core.global.dto.ApiResponse<CursorPageResponse<UserCommentItem>>> getMyCommentList(
            @Parameter(description = "페이지 크기(1~100)", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "응답의 nextCursor를 그대로 입력(첫 페이지는 비움)",
                    example = "eyJ0IjoiMjAyNS0wOC0wMVQxMjozNDo1NloiLCJpZCI6OTg3NjV9")
            @RequestParam(required = false) String cursor
    ) {
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity.ok(
                core.global.dto.ApiResponse.success(
                        commentService.getMyCommentList(size, cursor)
                )
        );
    }


    @Operation(summary = "댓글 차단", description = "댓글을 차단합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음", content = @Content)
    })
    @PostMapping("/comments/{commentId}/block")
    public ResponseEntity<core.global.dto.ApiResponse<?>> blockUser(
            @PathVariable @Positive Long commentId
    ) {
        commentService.blockUser(commentId);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(core.global.dto.ApiResponse.success("차단 성공"));
    }
}
