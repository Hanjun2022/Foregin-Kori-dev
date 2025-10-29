package core.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "익명 글쓰기 가능 여부 응답")
public record PostWriteAnonymousAvailableResponse(
        @Schema(description = "익명 작성 가능 여부", example = "true")
        Boolean isAnonymousAvaliable
) {
}