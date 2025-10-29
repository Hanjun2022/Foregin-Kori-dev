package core.domain.bookmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "BookmarkListResponse", description = "북마크 상세 항목 응답")
public record BookmarkItem(
        @Schema(description = "북마크 ID", example = "100")
        Long bookmarkId,

        @Schema(description = "게시물 ID", example = "100")
        Long postId,

        @Schema(description = "작성자 이름", example = "alice")
        String authorName,

        @Schema(description = "게시글 내용", example = "안녕하세요, 첫 글입니다.")
        String content,

        @Schema(description = "좋아요 여부", example = "true")
        Boolean isLiked,

        @Schema(description = "좋아요 개수", example = "25")
        Long likeCount,

        @Schema(description = "댓글 개수", example = "12")
        Long commentCount,

        @Schema(description = "조회수", example = "530")
        Long checkCount,

        @Schema(description = "현재 사용자가 북마크 여부", example = "true")
        Boolean isMarked,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/u/alice.png")
        String userImage,

        @Schema(description = "게시글 이미지 목록", example = "[\"https://cdn.example.com/p/1.jpg\", \"https://cdn.example.com/p/2.jpg\"]")
        List<String> postImages
) {}