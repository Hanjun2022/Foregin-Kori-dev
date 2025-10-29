package core.domain.board.dto;

import core.global.enums.BoardCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "BoardResponse", description = "게시글 카드 응답")
public record BoardItem(
        @Schema(description = "게시글 ID", example = "123")
        Long postId,

        @Schema(description = "내용 미리보기", example = "안녕하세요! 첫 글입니다.")
        String contentPreview,

        @Schema(description = "작성자 ID", example = "123")
        Long authorId,

        @Schema(description = "작성자 이름 (익명일 경우 null)", nullable = true, example = "alice")
        String authorName,

        @Schema(description = "게시판 카테고리")
        BoardCategory boardCategory,

        @Schema(description = "작성 시간(UTC)", type = "string", format = "date-time", example = "2025-08-20T12:34:56Z")
        Instant createdAt,

        @Schema(description = "좋아요 여부", example = "true")
        Boolean isLiked,

        @Schema(description = "좋아요 수", example = "10")
        Long likeCount,

        @Schema(description = "댓글 수", example = "5")
        Long commentCount,

        @Schema(description = "조회 수", example = "1234")
        Long viewCount,

        @Schema(description = "작성자 프로필 이미지 URL(익명 시 null)", nullable = true, example = "https://cdn.example.com/u/alice.png")
        String userImageUrl,

        @Schema(description = "콘텐츠 이미지 URL(없으면 null)", nullable = true, example = "https://cdn.example.com/p/123.jpg")
        String contentImageUrl,

        @Schema(description = "콘텐츠 이미지 갯수 ", nullable = true, example = "2")
        Integer imageCount,

        @Schema(description = "인기 점수(인기 정렬 시 커서용, 없으면 null)", nullable = true, example = "987654321")
        Long score
) { }