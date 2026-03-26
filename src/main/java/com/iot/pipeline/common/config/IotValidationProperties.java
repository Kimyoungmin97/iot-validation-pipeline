package com.iot.pipeline.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * IoT 검증 설정 프로퍼티
 *
 * 설계 의도:
 * - 센서별 유효 범위를 application.yml에서 주입받아 하드코딩을 제거
 * - 신규 센서 타입 추가 시 yml만 수정하면 되므로 OCP(개방-폐쇄 원칙) 준수
 * - @ConfigurationProperties를 사용해 타입 안전한 설정 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "iot.validation")
public class IotValidationProperties {

    private String topicPrefix = "iot/devices/";

    /** key: 센서 타입 이름(temperature, humidity 등), value: 범위 설정 */
    private Map<String, SensorRange> sensors = new HashMap<>();

    @Getter
    @Setter
    public static class SensorRange {
        private double min;
        private double max;
    }
}
