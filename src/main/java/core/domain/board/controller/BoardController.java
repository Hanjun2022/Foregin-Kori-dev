package core.domain.board.controller;

import core.domain.board.dto.BoardItem;
import core.domain.board.dto.CategoryListResponse;
import core.domain.board.service.BoardService;
import core.domain.post.dto.PostWriteAnonymousAvailableResponse;
import core.domain.post.service.PostService;
import core.global.enums.SortOption;
import core.global.metrics.FeatureUsageMetrics;
import core.global.pagination.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Board")
@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final PostService postService;
    private final BoardService boardService;
    private final FeatureUsageMetrics featureUsageMetrics;

    public BoardController(PostService postService, BoardService boardService, FeatureUsageMetrics featureUsageMetrics) {
        this.postService = postService;
        this.boardService = boardService;
        this.featureUsageMetrics = featureUsageMetrics;
    }

    @Operation(
            summary = "게시글 목록 조회",
            description = """
          - 무한스크롤: 응답의 `nextCursor`를 다음 호출의 `cursor`로 그대로 전달하세요.
          - 정렬: LATEST(기본) | POPULAR
          - 전체 게시글 조회: boardId=1 → ALL
          
          요청 예시
          1) 첫 페이지:
             GET /api/v1/boards/10/posts?sort=LATEST&size=20
          
          2) 다음 페이지(커서 포함):
             GET /api/v1/boards/10/posts?sort=LATEST&size=20&cursor=eyJ0IjoiMjAyNS0wOC0yMVQxMjowMDowMFoiLCJpZCI6MTAxfQ
          
          3) curl:
             curl -G 'https://api.example.com/api/v1/boards/10/posts' \\
                  --data-urlencode 'sort=LATEST' \\
                  --data-urlencode 'size=20' \\
                  --data-urlencode 'cursor=eyJ0IjoiMjAyNS0wOC0yMVQxMjowMDowMFoiLCJpZCI6MTAxfQ'
          
          4) JS(fetch):
             fetch('/api/v1/boards/10/posts?sort=LATEST&size=20&cursor=' + encodeURIComponent(nextCursor))
          
          5) axios:
             axios.get('/api/v1/boards/10/posts', { params: { sort: 'LATEST', size: 20, cursor: nextCursor } })
          """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청(유효하지 않은 파라미터 등)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "잘못된 커서",
                                    value = "{ \"code\": \"INVALID_CURSOR\", \"message\": \"cursor 형식이 올바르지 않습니다.\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 게시판",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{ \"code\": \"BOARD_NOT_FOUND\", \"message\": \"요청한 게시판을 찾을 수 없습니다.\" }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json"
                    )
            )
    })
    @GetMapping("/{boardId}/posts")
    public ResponseEntity<core.global.dto.ApiResponse<CursorPageResponse<BoardItem>>> getPostList(
            @PathVariable Long boardId,
            @Parameter(description = "정렬 옵션", example = "LATEST") @RequestParam(defaultValue = "LATEST") SortOption sort,
            @Parameter(description = "응답의 nextCursor를 그대로 입력(첫 페이지는 비움)", example = "eyJ0IjoiMjAyNS0wOC0yMVQxMjowMDowMFoiLCJpZCI6MTAxfQ")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity.ok(
                core.global.dto.ApiResponse.success(
                        postService.getPostList(boardId, sort, cursor, size)
                ));
    }

    @Operation(
            summary = "카테고리 목록",
            description = "카테고리 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            // 리스트 응답 예시
                            examples = @ExampleObject(
                                    name = "성공 예시",
                                    value = """
                                        {
                                          "success": true,
                                          "data": [
                                            { "categoryId": 10, "name": "공지" },
                                            { "categoryId": 11, "name": "자유게시판" },
                                            { "categoryId": 12, "name": "QnA" }
                                          ]
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "보드 없음", content = @Content)
    })
    @GetMapping("/categories")
    public ResponseEntity<core.global.dto.ApiResponse<List<CategoryListResponse>>> getCategories() {
        return ResponseEntity.ok(
                core.global.dto.ApiResponse.success(
                        boardService.getCategories()
                )
        );
    }

    @Operation(
            summary = "익명 글쓰기 가능 여부",
            description = "선택한 보드에서 익명 작성이 가능한지 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostWriteAnonymousAvailableResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "boardId": 10,
                                            "anonymousWritable": true
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "보드 없음", content = @Content)
    })
    @GetMapping("/{boardId}/write-options")
    public ResponseEntity<core.global.dto.ApiResponse<PostWriteAnonymousAvailableResponse>> getWriteOptions(
            @Parameter(description = "보드 ID", example = "10")
            @PathVariable @Positive(message = "boardId는 양수여야 합니다.") Long boardId
    ) {
        return ResponseEntity.ok(
                core.global.dto.ApiResponse.success(
                        boardService.isAnonymousAvaliable(boardId)
                )
        );
    }
}
