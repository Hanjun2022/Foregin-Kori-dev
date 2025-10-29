package core.domain.bookmark.controller;

import core.domain.bookmark.dto.BookmarkItem;
import core.domain.bookmark.service.BookmarkService;
import core.global.metrics.FeatureUsageMetrics;
import core.global.pagination.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Bookmark", description = "북마크 API")
@RestController
@RequestMapping("/api/v1")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final FeatureUsageMetrics featureUsageMetrics;

    public BookmarkController(BookmarkService bookmarkService, FeatureUsageMetrics featureUsageMetrics) {
        this.bookmarkService = bookmarkService;
        this.featureUsageMetrics = featureUsageMetrics;
    }

    @Operation(
            summary = "게시글 북마크 추가",
            description = "현재 로그인한 사용자가 지정한 게시글을 북마크에 추가합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "성공 (내용 없음)")
    @PutMapping("/posts/{postId}/bookmarks/me")
    public ResponseEntity<Void> addBookmark(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId
    ) {
        bookmarkService.addBookmark(postId);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "게시글 북마크 제거",
            description = "현재 로그인한 사용자가 지정한 게시글을 북마크에서 제거합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "성공 (내용 없음)")
    @DeleteMapping("/posts/{postId}/bookmarks/me")
    public ResponseEntity<Void> removeBookmark(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId
    ) {
        bookmarkService.removeBookmark(postId);
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "내 북마크 목록",
            description = """
                      - 정렬: bookmarkId DESC
                      - 무한스크롤: 응답의 `nextCursor`를 다음 호출의 `cursor`로 그대로 전달
                    
                      요청 예시
                      1) 첫 페이지:
                         GET /api/v1/boards/my/bookmarks?size=20
                    
                      2) 다음 페이지:
                         GET /api/v1/boards/my/bookmarks?size=20&cursor=eyJpZCI6NTQ5fQ
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "items": [
                                          {
                                            "bookmarkId": 555,
                                            "postId": 1,
                                            "authorName": "Anonymity",
                                            "content": "내용 프리뷰...",
                                            "likeCount": 10,
                                            "commentCount": 2,
                                            "checkCount": 3,
                                            "isMarked": true,
                                            "userImage": "https://...",
                                            "postImages": ["https://.../1.png","https://.../2.png"]
                                          }
                                        ],
                                        "hasNext": true,
                                        "nextCursor": "eyJpZCI6NTQ5fQ"
                                      }
                                    }
                                    """
                    )
            ))
    })
    @GetMapping("/my/bookmarks")
    public ResponseEntity<core.global.dto.ApiResponse<CursorPageResponse<BookmarkItem>>> getMyBookmarks(
            @Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "응답의 nextCursor를 그대로 입력(첫 페이지는 비움)", example = "eyJpZCI6NTQ5fQ")
            @RequestParam(required = false) String cursor
    ) {
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity.ok(core.global.dto.ApiResponse.success(
                bookmarkService.getMyBookmarks(size, cursor)
        ));
    }
}