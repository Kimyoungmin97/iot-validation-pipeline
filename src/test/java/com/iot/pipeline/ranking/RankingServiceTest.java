package com.iot.pipeline.ranking;

import com.iot.pipeline.ranking.dto.RankingResponse;
import com.iot.pipeline.ranking.entity.SearchKeyword;
import com.iot.pipeline.ranking.repository.SearchKeywordRepository;
import com.iot.pipeline.ranking.service.RankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * RankingService 단위 테스트 (Redis Mockito)
 *
 * 전략:
 * - RedisTemplate을 Mockito로 모킹해 Redis 없이 서비스 로직 테스트
 * - ZSetOperations 체인 모킹 방법 시연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService 단위 테스트")
class RankingServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ZSetOperations<String, Object> zSetOperations;
    @Mock private SearchKeywordRepository searchKeywordRepository;

    @InjectMocks
    private RankingService rankingService;

    @Test
    @DisplayName("검색어 기록 시 ZINCRBY 호출 및 DB 저장")
    void record_search_increments_redis_score_and_saves_db() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.incrementScore(anyString(), anyString(), anyDouble())).willReturn(1.0);
        given(searchKeywordRepository.findByKeyword("temperature")).willReturn(Optional.empty());
        given(searchKeywordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        rankingService.recordSearch("temperature");

        // then
        verify(zSetOperations).incrementScore("search:ranking", "temperature", 1.0);
        verify(searchKeywordRepository).save(any(SearchKeyword.class));
    }

    @Test
    @DisplayName("기존 키워드 재검색 시 DB 카운트 증가")
    void record_existing_keyword_increments_count() {
        // given
        SearchKeyword existing = SearchKeyword.builder().keyword("temperature").build();
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.incrementScore(anyString(), anyString(), anyDouble())).willReturn(2.0);
        given(searchKeywordRepository.findByKeyword("temperature")).willReturn(Optional.of(existing));
        given(searchKeywordRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        rankingService.recordSearch("temperature");

        // then — incrementCount() 호출 여부는 searchCount가 증가했는지로 간접 검증
        verify(searchKeywordRepository).save(existing);
        assertThat(existing.getSearchCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Top 랭킹 조회 시 Redis ZSet 결과를 순서대로 변환")
    void get_top_rankings_returns_sorted_results() {
        // given
        Set<ZSetOperations.TypedTuple<Object>> tuples = new LinkedHashSet<>();
        tuples.add(mockTuple("temperature", 100.0));
        tuples.add(mockTuple("humidity", 80.0));
        tuples.add(mockTuple("pressure", 50.0));

        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(eq("search:ranking"), eq(0L), eq(9L)))
                .willReturn(tuples);

        // when
        RankingResponse response = rankingService.getTopRankings();

        // then
        assertThat(response.getRankings()).hasSize(3);
        assertThat(response.getRankings().get(0).getKeyword()).isEqualTo("temperature");
        assertThat(response.getRankings().get(0).getRank()).isEqualTo(1);
        assertThat(response.getRankings().get(1).getKeyword()).isEqualTo("humidity");
    }

    @Test
    @DisplayName("Redis 데이터 없으면 빈 랭킹 반환")
    void empty_redis_returns_empty_ranking() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(anyString(), anyLong(), anyLong()))
                .willReturn(null);

        // when
        RankingResponse response = rankingService.getTopRankings();

        // then
        assertThat(response.getRankings()).isEmpty();
    }

    @Test
    @DisplayName("검색 횟수 조회 — 키워드 없으면 0 반환")
    void get_search_count_returns_zero_when_not_found() {
        // given
        given(searchKeywordRepository.findByKeyword("unknown")).willReturn(Optional.empty());

        // when
        long count = rankingService.getSearchCount("unknown");

        // then
        assertThat(count).isZero();
    }

    /** ZSetOperations.TypedTuple 목 객체 생성 헬퍼 */
    private ZSetOperations.TypedTuple<Object> mockTuple(String value, double score) {
        ZSetOperations.TypedTuple<Object> tuple = mock(ZSetOperations.TypedTuple.class);
        given(tuple.getValue()).willReturn(value);
        given(tuple.getScore()).willReturn(score);
        return tuple;
    }
}
