package core.domain.user.controller;


import core.domain.user.dto.UserProfileResponse;
import core.domain.user.service.RecommenderService;
import core.global.metrics.FeatureUsageMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "친구 추천 ", description = "친구 추천 API")
@RestController
@RequestMapping("/api/v1/commend")
@RequiredArgsConstructor
@Slf4j
public class FriendCommendController {
    private final RecommenderService recommenderService;
    private final FeatureUsageMetrics featureUsageMetrics;

    @GetMapping("/content-based")
    @Operation(summary = "친구 추천 기능", description = "콘텐츠 기반 필터링으로 친구를 추천합니다.")
    public ResponseEntity<List<UserProfileResponse>> recommend(
            @RequestParam(defaultValue = "50") int limit
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        List<UserProfileResponse> list = recommenderService.recommendForUser(auth, limit);
        log.info(">>>> 최종 반환 유저: {}", list);
        featureUsageMetrics.recordFollowUsage();

        return ResponseEntity.ok(list);
    }

}
