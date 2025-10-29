package core.global.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CursorPageResponse", description = "커서 기반 무한스크롤 공통 응답")
public record CursorPageResponse<T>(
        @Schema(description = "항목 리스트(제네릭)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<T> items,

        @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasNext,

        @Schema(description = "다음 페이지를 위한 불투명 커서(없으면 null)", example = "eyJ0IjoiMjAyNS0wOC0yMFQxMjozMzowMFoiLCJpZCI6MTIwLCJzYyI6MTIzNDU2fQ", nullable = true)
        String nextCursor
) {}