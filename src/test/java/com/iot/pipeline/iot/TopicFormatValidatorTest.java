package com.iot.pipeline.iot;

import com.iot.pipeline.common.config.IotValidationProperties;
import com.iot.pipeline.iot.validator.TopicFormatValidator;
import com.iot.pipeline.iot.validator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TopicFormatValidator 단위 테스트
 *
 * 전략:
 * - Spring Context 없이 순수 Java 객체 테스트 → 빠른 실행
 * - 정상 케이스와 경계 케이스(빈 문자열, 잘못된 prefix 등) 모두 검증
 */
@DisplayName("TopicFormatValidator 단위 테스트")
class TopicFormatValidatorTest {

    private TopicFormatValidator validator;

    @BeforeEach
    void setUp() {
        IotValidationProperties properties = new IotValidationProperties();
        properties.setTopicPrefix("iot/devices/");
        validator = new TopicFormatValidator(properties);
    }

    @Test
    @DisplayName("정상 topic은 검증 성공")
    void valid_topic_passes() {
        ValidationResult result = validator.validate("iot/devices/device-001/temperature");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("null topic은 실패")
    void null_topic_fails() {
        ValidationResult result = validator.validate(null);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getMessage()).contains("비어있습니다");
    }

    @Test
    @DisplayName("빈 문자열 topic은 실패")
    void blank_topic_fails() {
        ValidationResult result = validator.validate("   ");
        assertThat(result.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("잘못된 prefix는 실패")
    void wrong_prefix_fails() {
        ValidationResult result = validator.validate("wrong/prefix/device-001/temperature");
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getMessage()).contains("iot/devices/");
    }

    @Test
    @DisplayName("sensorType 없는 topic은 실패")
    void missing_sensor_type_fails() {
        ValidationResult result = validator.validate("iot/devices/device-001");
        assertThat(result.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("deviceId 추출 정상 동작")
    void extract_device_id() {
        String deviceId = validator.extractDeviceId("iot/devices/device-001/temperature");
        assertThat(deviceId).isEqualTo("device-001");
    }

    @Test
    @DisplayName("sensorType 추출 정상 동작")
    void extract_sensor_type() {
        String sensorType = validator.extractSensorType("iot/devices/device-001/temperature");
        assertThat(sensorType).isEqualTo("temperature");
    }
}
