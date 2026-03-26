package com.iot.pipeline.integration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * 통합 테스트용 임베디드 Redis 설정
 *
 * 설계 의도:
 * - 실제 Redis 없이 통합 테스트를 실행하기 위해 임베디드 Redis 사용
 * - @TestConfiguration: 프로덕션 컨텍스트에는 영향 없이 테스트 컨텍스트에만 적용
 * - 포트 6370을 사용해 개발 환경의 Redis(6379)와 충돌 방지
 */
@TestConfiguration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
