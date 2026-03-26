package com.iot.pipeline.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 인기 검색어 랭킹 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {

    private List<RankItem> rankings;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankItem {
        private int rank;
        private String keyword;
        private Double score;
    }
}
