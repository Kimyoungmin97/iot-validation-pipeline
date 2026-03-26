package com.iot.pipeline.ranking.controller;

import com.iot.pipeline.common.response.ApiResponse;
import com.iot.pipeline.ranking.dto.RankingResponse;
import com.iot.pipeline.ranking.service.RankingService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인기 검색어 랭킹 컨트롤러
 */
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * 검색어 기록
     * POST /api/ranking/search?keyword=temperature
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Void>> search(@RequestParam @NotBlank String keyword) {
        rankingService.recordSearch(keyword);
        return ResponseEntity.ok(ApiResponse.ok("검색어가 기록되었습니다.", null));
    }

    /**
     * 인기 검색어 Top 10 조회
     * GET /api/ranking/top
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<RankingResponse>> getTopRankings() {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.getTopRankings()));
    }

    /**
     * 특정 키워드 검색 횟수 조회 (Cache-Aside 패턴 시연)
     * GET /api/ranking/count?keyword=temperature
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getSearchCount(@RequestParam @NotBlank String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.getSearchCount(keyword)));
    }
}
