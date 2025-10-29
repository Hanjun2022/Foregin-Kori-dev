package core.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "UserCommentItem", description = "사용자가 작성한 댓글 아이템")
public record UserCommentItem(
        @Schema(description = "댓글 ID", example = "99")
        Long commentId,

        @Schema(description = "게시글 ID", example = "99")
        Long postId,

        @Schema(description = "게시글 내용", example = "게시글 본문입니다.")
        String postContent,

        @Schema(description = "댓글 내용", example = "저도 동의합니다!")
        String commentContent,

        @Schema(description = "작성일", type = "string", format = "date-time", example = "2025-08-21T14:00:00Z")
        Instant createdAt
) {}