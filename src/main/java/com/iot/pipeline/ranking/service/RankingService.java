package com.iot.pipeline.ranking.service;

import com.iot.pipeline.ranking.dto.RankingResponse;
import com.iot.pipeline.ranking.dto.RankingResponse.RankItem;
import com.iot.pipeline.ranking.entity.SearchKeyword;
import com.iot.pipeline.ranking.repository.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 인기 검색어 랭킹 서비스
 *
 * 설계 의도:
 * 1. Redis Sorted Set(ZINCRBY) 활용:
 *    - ZINCRBY: 검색할 때마다 해당 키워드의 score를 1 증가
 *    - ZREVRANGE: score 내림차순으로 상위 N개 조회
 *    - O(log N) 삽입, O(log N + K) 조회로 DB 기반 정렬보다 성능 우수
 *
 * 2. DB 병행 저장:
 *    - Redis는 인메모리라 재시작 시 데이터 손실 가능
 *    - DB에 검색 횟수를 영속화해 Redis 장애 시 복구 기반 마련
 *
 * 3. Cache-Aside 패턴 (@Cacheable):
 *    - 장비 정보 등 자주 조회되지만 잘 변하지 않는 데이터는 DB 조회 결과를 캐싱
 *    - 캐시 미스 시 DB 조회 후 캐시에 저장, 이후 요청은 캐시에서 응답
 *
 * 면접 포인트:
 * "왜 Sorted Set을 썼나요?"
 * → 일반 Hash나 String으로 저장 후 애플리케이션에서 정렬하면 모든 키워드를 메모리에 올려야 합니다.
 *   Sorted Set은 Redis 내부에서 정렬을 유지하므로 상위 N개만 가져오는 ZREVRANGE가 O(log N + K)입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private static final String RANKING_KEY = "search:ranking";
    private static final int TOP_RANK_SIZE = 10;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchKeywordRepository searchKeywordRepository;

    /**
     * 검색어 기록 및 랭킹 점수 갱신
     *
     * @param keyword 검색어
     */
    @Transactional
    public void recordSearch(String keyword) {
        // Redis Sorted Set에 점수 1 증가 (없으면 자동 생성)
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, keyword, 1.0);
        log.debug("[Ranking] 검색어 기록 — keyword={}", keyword);

        // DB에도 영속화 (upsert 방식)
        SearchKeyword entity = searchKeywordRepository.findByKeyword(keyword)
                .orElseGet(() -> SearchKeyword.builder().keyword(keyword).build());

        if (entity.getId() != null) {
            entity.incrementCount();
        }
        searchKeywordRepository.save(entity);
    }

    /**
     * 인기 검색어 Top N 조회
     * Redis Sorted Set에서 score 내림차순으로 조회
     */
    public RankingResponse getTopRankings() {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet()
                        .reverseRangeWithScores(RANKING_KEY, 0, TOP_RANK_SIZE - 1);

        List<RankItem> items = new ArrayList<>();
        if (tuples != null) {
            int rank = 1;
            for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                items.add(RankItem.builder()
                        .rank(rank++)
                        .keyword(String.valueOf(tuple.getValue()))
                        .score(tuple.getScore())
                        .build());
            }
        }

        return RankingResponse.builder().rankings(items).build();
    }

    /**
     * Cache-Aside 패턴 예시: 특정 키워드의 총 검색 횟수 조회
     * @Cacheable: 같은 keyword로 반복 조회 시 DB 대신 Redis 캐시에서 응답
     *
     * 설계 의도:
     * - "cache" 이름 캐시에 TTL 5분 적용 (RedisConfig에서 설정)
     * - 캐시 키: "keyword::{keyword}" 형식으로 자동 생성
     */
    @Cacheable(value = "keyword", key = "#keyword")
    @Transactional(readOnly = true)
    public long getSearchCount(String keyword) {
        log.debug("[Cache-Aside] DB 조회 — keyword={}", keyword);
        return searchKeywordRepository.findByKeyword(keyword)
                .map(SearchKeyword::getSearchCount)
                .orElse(0L);
    }

    /**
     * 특정 키워드의 현재 Redis 랭킹 점수 조회
     */
    public Double getScore(String keyword) {
        return redisTemplate.opsForZSet().score(RANKING_KEY, keyword);
    }
}
