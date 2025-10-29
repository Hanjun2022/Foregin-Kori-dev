package core.domain.post.controller;

import core.domain.post.dto.PostDetailResponse;
import core.domain.post.dto.SearchResultView;
import core.domain.post.dto.SuggestClickRequest;
import core.domain.post.service.PostSearchService;
import core.domain.post.service.PostService;
import core.domain.post.service.RecentSearchRedisService;
import core.domain.post.service.SuggestMemoryIndex;
import core.global.dto.ApiResponse;
import core.global.metrics.FeatureUsageMetrics;
import core.global.pagination.CursorPageResponse;
import core.global.service.SimpleKeywordExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "게시글 검색/자동완성/최근 검색어 API")
public class PostSearchController {

    private final PostService postService;
    private final PostSearchService searchService;
    private final RecentSearchRedisService recentService;
    private final FeatureUsageMetrics featureUsageMetrics;
    private final SuggestMemoryIndex memoryIndex;
    private final SimpleKeywordExtractor keywordExtractor;

    @Operation(summary = "게시글 검색", description = "커서 페이지네이션 지원")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @GetMapping("/{boardId}/posts")
    public ResponseEntity<ApiResponse<CursorPageResponse<SearchResultView>>> getPostList(
            @RequestParam String q,
            @PathVariable Long boardId,
            @Parameter(description = "응답의 nextCursor를 그대로 입력(첫 페이지는 비움)", example = "eyJ0IjoiMjAyNS0wOC0yMVQxMjowMDowMFoiLCJpZCI6MTAxfQ")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        featureUsageMetrics.recordCommunityUsage();
        return ResponseEntity.ok(
                core.global.dto.ApiResponse.success(
                        searchService.search(q, boardId, cursor, size)
                ));
    }

    @Operation(summary = "검색결과 상세페이지",
            description = "사용자가 검색결과를 클릭/열람했을 때 호출하여 인기(pop) 점수를 반영하고 상세페이지를 제공합니다.")
    @GetMapping("/{boardId}/posts/{postId}")
    public ResponseEntity<core.global.dto.ApiResponse<PostDetailResponse>> resultClicked(
            @Parameter(description = "보드 ID(1=전체)") @PathVariable Long boardId,
            @Parameter(description = "게시글 ID", example = "123") @PathVariable @Positive Long postId,
            @RequestParam Boolean translate
    ) {
        featureUsageMetrics.recordCommunityUsage();
        PostDetailResponse postDetail = postService.getPostDetail(postId, translate);

        extractedKeyword(postDetail.content(),1);

        return ResponseEntity.ok(core.global.dto.ApiResponse.success(postDetail));
    }

    @Operation(summary = "자동완성 제안", description = "메모리+DB 하이브리드")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @GetMapping("/{boardId}/suggest")
    public List<String> suggestByBoard(@PathVariable Long boardId, @RequestParam("q") String q) {
        featureUsageMetrics.recordCommunityUsage();
        return searchService.suggest(q, boardId);
    }

    @Operation(summary = "자동완성 클릭 기록")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @PostMapping("/clicked")
    public void clicked(@RequestBody @Valid SuggestClickRequest body) {
        extractedKeyword(body.text(),1);
    }

    @Operation(summary = "최근 검색어 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @GetMapping("/recent")
    public List<String> recent() {
        featureUsageMetrics.recordCommunityUsage();
        return recentService.list();
    }

    @Operation(summary = "최근 검색어 단건 삭제")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @DeleteMapping("/recent")
    public void deleteRecent(@RequestParam String q) {
        featureUsageMetrics.recordCommunityUsage();
        recentService.remove(q);
    }

    @Operation(summary = "최근 검색어 전체 삭제")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @DeleteMapping("/recent/all")
    public void clearRecent() {
        featureUsageMetrics.recordCommunityUsage();
        recentService.clear();
    }

    private void extractedKeyword(String content, int score) {
        if (content != null && !content.isBlank()) {
            // 상위 K만 반영
            int K = 8;
            var phrases = keywordExtractor.extract(content, K);
            phrases.forEach(p -> memoryIndex.upsert(p, score));
        }
    }
}