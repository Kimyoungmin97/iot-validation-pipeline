package com.iot.pipeline.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.pipeline.iot.dto.ParsedPayload;
import com.iot.pipeline.iot.validator.PayloadParseValidator;
import com.iot.pipeline.iot.validator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PayloadParseValidator 단위 테스트
 */
@DisplayName("PayloadParseValidator 단위 테스트")
class PayloadParseValidatorTest {

    private PayloadParseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PayloadParseValidator(new ObjectMapper());
    }

    @Test
    @DisplayName("정상 payload는 검증 성공")
    void valid_payload_passes() {
        String payload = """
                {"value": 23.5, "unit": "celsius", "timestamp": 1711234567890}
                """;
        ValidationResult result = validator.validate(payload);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("유효한 payload 파싱 결과 확인")
    void parse_valid_payload() {
        String payload = """
                {"value": 23.5, "unit": "celsius", "timestamp": 1711234567890}
                """;
        validator.validate(payload);
        ParsedPayload parsed = validator.parse(payload);

        assertThat(parsed.getValue()).isEqualTo(23.5);
        assertThat(parsed.getUnit()).isEqualTo("celsius");
        assertThat(parsed.getTimestamp()).isEqualTo(1711234567890L);
    }

    @Test
    @DisplayName("잘못된 JSON 형식은 실패")
    void invalid_json_fails() {
        ValidationResult result = validator.validate("not-a-json");
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getMessage()).contains("유효한 JSON");
    }

    @Test
    @DisplayName("value 필드 누락 시 실패")
    void missing_value_field_fails() {
        String payload = """
                {"unit": "celsius"}
                """;
        ValidationResult result = validator.validate(payload);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getMessage()).contains("value");
    }

    @Test
    @DisplayName("value가 문자열이면 실패")
    void string_value_fails() {
        String payload = """
                {"value": "hot", "unit": "celsius"}
                """;
        ValidationResult result = validator.validate(payload);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getMessage()).contains("숫자");
    }

    @Test
    @DisplayName("unit 필드 누락 시 실패")
    void missing_unit_field_fails() {
        String payload = """
                {"value": 23.5}
                """;
        ValidationResult result = validator.validate(payload);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getMessage()).contains("unit");
    }

    @Test
    @DisplayName("null payload는 실패")
    void null_payload_fails() {
        ValidationResult result = validator.validate(null);
        assertThat(result.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("timestamp 없어도 현재 시간으로 파싱 성공")
    void parse_without_timestamp_uses_current_time() {
        String payload = """
                {"value": 23.5, "unit": "celsius"}
                """;
        validator.validate(payload);
        ParsedPayload parsed = validator.parse(payload);

        assertThat(parsed.getTimestamp()).isPositive();
    }
}
