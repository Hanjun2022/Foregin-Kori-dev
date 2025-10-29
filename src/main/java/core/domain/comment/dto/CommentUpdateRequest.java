package core.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 수정 요청")
public record CommentUpdateRequest(
        @Schema(description = "수정할 댓글 내용", example = "수정된 댓글입니다.")
        String content
) {
}