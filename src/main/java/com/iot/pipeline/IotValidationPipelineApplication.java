package com.iot.pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * IoT 데이터 검증 파이프라인 애플리케이션
 *
 * @EnableCaching: Spring Cache 추상화를 활성화해 @Cacheable 등의 어노테이션이 동작하도록 함
 */
@SpringBootApplication
@EnableCaching
public class IotValidationPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotValidationPipelineApplication.class, args);
    }
}
