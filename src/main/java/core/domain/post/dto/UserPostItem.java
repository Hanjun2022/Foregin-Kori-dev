package core.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "UserPostResponse", description = "사용자가 작성한 게시글 응답 DTO")
public record UserPostItem(
        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "게시글 내용", example = "오늘 다녀온 맛집 공유해요!")
        String content,

        @Schema(description = "게시글 작성 시간", type = "string", format = "date-time", example = "2025-08-21T14:00:00Z")
        Instant createdAt,

        @Schema(description = "좋아요 여부", example = "true")
        Boolean isLiked,

        @Schema(description = "좋아요 개수", example = "15")
        Long likeCount,

        @Schema(description = "댓글 개수", example = "5")
        Long commentCount,

        @Schema(description = "조회수", example = "340")
        Long viewCount,

        @Schema(description = "게시글 대표 이미지 URL", example = "https://cdn.example.com/p/123.jpg", nullable = true)
        String imageUrl,

        @Schema(description = "콘텐츠 이미지 갯수 ", nullable = true, example = "2")
        Integer imageCount
) {}
