package com.iot.pipeline.ranking.repository;

import com.iot.pipeline.ranking.entity.SearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {
    Optional<SearchKeyword> findByKeyword(String keyword);
}
