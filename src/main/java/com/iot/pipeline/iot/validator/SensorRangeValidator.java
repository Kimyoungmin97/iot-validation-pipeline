package com.iot.pipeline.iot.validator;

import com.iot.pipeline.common.config.IotValidationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 3단계: 센서 값 물리적 범위 검사기
 *
 * 설계 의도:
 * - 센서 타입별 물리적 유효 범위를 application.yml에서 읽어 검증
 * - 알려지지 않은 센서 타입은 경고 로그만 남기고 통과시킴
 *   (신규 장비를 배포한 직후에도 파이프라인이 중단되지 않도록)
 * - 향후 "알 수 없는 센서 타입도 거부"하는 정책으로 쉽게 변경 가능한 구조
 *
 * 면접 포인트:
 * "알 수 없는 센서 타입을 왜 통과시키나요?"
 * → 실제 IoT 환경에서는 장비 펌웨어 업데이트로 새 센서 타입이 먼저 배포될 수 있습니다.
 *   서버 설정 배포 전에 데이터가 들어와도 파이프라인 전체가 중단되지 않도록 유연하게 설계했습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorRangeValidator {

    private final IotValidationProperties properties;

    /**
     * @param sensorType 센서 타입 (예: temperature, humidity)
     * @param value      센서 측정값
     */
    public ValidationResult validate(String sensorType, double value) {
        IotValidationProperties.SensorRange range = properties.getSensors().get(sensorType);

        if (range == null) {
            // 미등록 센서 타입은 경고 로그 후 통과
            log.warn("[RangeCheck] 미등록 센서 타입 — sensorType={}, value={}", sensorType, value);
            return ValidationResult.success();
        }

        if (value < range.getMin() || value > range.getMax()) {
            return ValidationResult.failure(
                    String.format("센서 값이 유효 범위를 벗어났습니다. sensorType=%s, value=%.2f, 허용범위=[%.2f, %.2f]",
                            sensorType, value, range.getMin(), range.getMax()));
        }

        return ValidationResult.success();
    }
}
