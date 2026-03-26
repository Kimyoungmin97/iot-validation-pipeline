package com.iot.pipeline.iot.validator;

import com.iot.pipeline.common.config.IotValidationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 1단계: Topic 규격 검증기
 *
 * 설계 의도:
 * - MQTT Topic 형식: {prefix}{deviceId}/{sensorType}
 *   예: iot/devices/device-001/temperature
 * - prefix는 설정 파일에서 읽어 하드코딩 방지
 * - deviceId, sensorType이 존재하는지 구조적으로 검증
 */
@Component
@RequiredArgsConstructor
public class TopicFormatValidator implements IotValidator<String> {

    private final IotValidationProperties properties;

    @Override
    public ValidationResult validate(String topic) {
        if (topic == null || topic.isBlank()) {
            return ValidationResult.failure("topic이 비어있습니다.");
        }

        String prefix = properties.getTopicPrefix();
        if (!topic.startsWith(prefix)) {
            return ValidationResult.failure(
                    String.format("topic은 '%s'로 시작해야 합니다. 수신된 topic: %s", prefix, topic));
        }

        // prefix 이후 경로 파싱: {deviceId}/{sensorType}
        String remainder = topic.substring(prefix.length());
        String[] parts = remainder.split("/");

        if (parts.length < 2) {
            return ValidationResult.failure(
                    String.format("topic 형식이 올바르지 않습니다. 기대 형식: %s{{deviceId}}/{{sensorType}}", prefix));
        }

        if (parts[0].isBlank()) {
            return ValidationResult.failure("deviceId가 비어있습니다.");
        }

        if (parts[1].isBlank()) {
            return ValidationResult.failure("sensorType이 비어있습니다.");
        }

        return ValidationResult.success();
    }

    /**
     * Topic에서 deviceId 추출 (파이프라인 내 후속 단계에서 활용)
     */
    public String extractDeviceId(String topic) {
        String remainder = topic.substring(properties.getTopicPrefix().length());
        return remainder.split("/")[0];
    }

    /**
     * Topic에서 sensorType 추출
     */
    public String extractSensorType(String topic) {
        String remainder = topic.substring(properties.getTopicPrefix().length());
        return remainder.split("/")[1];
    }
}
