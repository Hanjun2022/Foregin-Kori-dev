package core.domain.post.dto;

import core.global.enums.BoardCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "게시글 상세 응답")
public record PostDetailResponse(
        @Schema(description = "ID", example = "1")
        Long postId,

        @Schema(description = "본문", example = "Hello~ I came to Korea from the U.S. as an exchange student")
        String content,

        @Schema(description = "작성자 ID", example = "1")
        Long authorId,

        @Schema(description = "작성자 표시명(익명이면 'Anonymity')", example = "Anonymity")
        String authorName,

        @Schema(description = "카테고리", example = "NEWS")
        BoardCategory boardCategory,

        @Schema(description = "작성 시각 (UTC)", type = "string", format = "date-time", example = "2025-08-13T09:41:00Z")
        Instant createdTime,

        @Schema(description = "채팅방 링크", example = "https://ko-ri.cloud/chatroom/10")
        String link,

        @Schema(description = "좋아요 여부", example = "true")
        Boolean isLiked,

        @Schema(description = "좋아요 수", example = "12")
        Long likeCount,

        @Schema(description = "댓글 수", example = "3")
        Long commentCount,

        @Schema(description = "조회 수", example = "257")
        Long viewCount,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/u/123/avatar.png")
        String userImageUrl,

        @Schema(description = "본문 내 이미지 URL 목록",
                example = "[\"https://cdn.example.com/p/1.png\",\"https://cdn.example.com/p/2.jpg\"]")
        List<String> contentImageUrls,

        @Schema(description = "이미지 수", example = "3")
        Integer imageCount
) {
    public PostDetailResponse(PostDetailResponse postDetail, String translatedContent) {
        this(
                postDetail.postId,
                translatedContent,
                postDetail.authorId,
                postDetail.authorName(),
                postDetail.boardCategory,
                postDetail.createdTime,
                postDetail.link,
                postDetail.isLiked,
                postDetail.likeCount,
                postDetail.commentCount,
                postDetail.viewCount,
                postDetail.userImageUrl,
                postDetail.contentImageUrls,
                postDetail.imageCount
        );
    }
}
