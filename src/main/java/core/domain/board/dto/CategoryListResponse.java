package core.domain.board.dto;

import core.global.enums.BoardCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리 목록 응답")
public record CategoryListResponse(
        @Schema(description = "카테고리", example = "EVENT")
        BoardCategory category
) {
}
