package core.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 작성 요청")
public record CommentWriteRequest(

        @Schema(description = "부모 댓글 ID (대댓글 작성 시 지정, 최상위 댓글이면 null)", example = "12345")
        Long parentId,

        @Schema(description = "익명 여부 (true = 익명, false = 실명)", example = "true")
        Boolean anonymous,

        @NotBlank
        @Size(max = 2000)
        @Schema(description = "댓글 본문", example = "좋은 글 감사합니다!", maxLength = 2000)
        String comment
) {
}