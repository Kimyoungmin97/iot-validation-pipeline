package com.iot.pipeline.ranking.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 검색 키워드 엔티티 (DB 영속성)
 *
 * 설계 의도:
 * - Redis Sorted Set은 캐시/랭킹용, DB는 키워드 메타데이터 영속화용으로 역할 분리
 * - Redis 장애 시에도 DB에서 검색어 데이터 복구 가능
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "search_keyword", indexes = {
        @Index(name = "idx_keyword", columnList = "keyword", unique = true)
})
public class SearchKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private Long searchCount = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public SearchKeyword(String keyword) {
        this.keyword = keyword;
        this.searchCount = 1L;
    }

    public void incrementCount() {
        this.searchCount++;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
