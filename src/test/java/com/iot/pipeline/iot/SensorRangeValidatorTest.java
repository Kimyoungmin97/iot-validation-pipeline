package com.iot.pipeline.iot;

import com.iot.pipeline.common.config.IotValidationProperties;
import com.iot.pipeline.iot.validator.SensorRangeValidator;
import com.iot.pipeline.iot.validator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensorRangeValidator 단위 테스트
 *
 * @ParameterizedTest 활용: 경계값(최솟값, 최댓값, 초과값 등)을 반복 검증
 */
@DisplayName("SensorRangeValidator 단위 테스트")
class SensorRangeValidatorTest {

    private SensorRangeValidator validator;

    @BeforeEach
    void setUp() {
        IotValidationProperties properties = new IotValidationProperties();

        Map<String, IotValidationProperties.SensorRange> sensors = new HashMap<>();
        IotValidationProperties.SensorRange tempRange = new IotValidationProperties.SensorRange();
        tempRange.setMin(-40.0);
        tempRange.setMax(125.0);
        sensors.put("temperature", tempRange);

        IotValidationProperties.SensorRange humidRange = new IotValidationProperties.SensorRange();
        humidRange.setMin(0.0);
        humidRange.setMax(100.0);
        sensors.put("humidity", humidRange);

        properties.setSensors(sensors);
        validator = new SensorRangeValidator(properties);
    }

    @ParameterizedTest(name = "temperature={1} → 정상")
    @CsvSource({"-40.0", "0.0", "25.5", "125.0"})
    @DisplayName("온도 정상 범위 내 값은 검증 성공")
    void temperature_valid_range(double value) {
        ValidationResult result = validator.validate("temperature", value);
        assertThat(result.isValid()).isTrue();
    }

    @ParameterizedTest(name = "temperature={0} → 실패")
    @CsvSource({"-40.1", "125.1", "999.0", "-100.0"})
    @DisplayName("온도 범위 초과 값은 검증 실패")
    void temperature_out_of_range(double value) {
        ValidationResult result = validator.validate("temperature", value);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getMessage()).contains("유효 범위를 벗어났습니다");
    }

    @ParameterizedTest(name = "humidity={0} → 정상")
    @CsvSource({"0.0", "50.0", "100.0"})
    @DisplayName("습도 정상 범위 내 값은 검증 성공")
    void humidity_valid_range(double value) {
        ValidationResult result = validator.validate("humidity", value);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("미등록 센서 타입은 경고 후 통과 (확장성)")
    void unknown_sensor_type_passes() {
        ValidationResult result = validator.validate("co2_sensor", 500.0);
        assertThat(result.isValid()).isTrue();
    }
}
