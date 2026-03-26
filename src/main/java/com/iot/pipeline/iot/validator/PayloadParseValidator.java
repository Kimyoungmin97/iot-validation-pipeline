package com.iot.pipeline.iot.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.pipeline.iot.dto.ParsedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2단계: Payload 파싱 및 필수 필드 검증기
 *
 * 설계 의도:
 * - JSON 파싱 실패와 필드 누락을 구분해서 에러 메시지 제공
 * - value 필드가 숫자 타입인지까지 검증해 범위 검사(3단계) 전 타입 안전성 보장
 * - ParsedPayload 반환 메서드를 별도로 제공해 파이프라인 서비스에서 재파싱 방지
 *
 * 예상 payload 형식:
 * {
 *   "value": 23.5,
 *   "unit": "celsius",
 *   "timestamp": 1711234567890
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayloadParseValidator implements IotValidator<String> {

    private final ObjectMapper objectMapper;

    @Override
    public ValidationResult validate(String payload) {
        if (payload == null || payload.isBlank()) {
            return ValidationResult.failure("payload가 비어있습니다.");
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.debug("[PayloadParse] JSON 파싱 실패: {}", payload);
            return ValidationResult.failure("payload가 유효한 JSON 형식이 아닙니다: " + e.getMessage());
        }

        // 필수 필드: value
        if (!node.has("value") || node.get("value").isNull()) {
            return ValidationResult.failure("payload에 'value' 필드가 없습니다.");
        }

        if (!node.get("value").isNumber()) {
            return ValidationResult.failure("payload의 'value' 필드는 숫자여야 합니다.");
        }

        // 필수 필드: unit
        if (!node.has("unit") || node.get("unit").isNull() || node.get("unit").asText().isBlank()) {
            return ValidationResult.failure("payload에 'unit' 필드가 없습니다.");
        }

        return ValidationResult.success();
    }

    /**
     * 유효성 검증을 통과한 payload를 ParsedPayload 객체로 변환
     * validate() 통과 후에만 호출해야 함
     */
    public ParsedPayload parse(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return ParsedPayload.builder()
                    .value(node.get("value").asDouble())
                    .unit(node.get("unit").asText())
                    .timestamp(node.has("timestamp") ? node.get("timestamp").asLong() : System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("파싱 실패 — validate() 통과 후 호출해야 합니다.", e);
        }
    }
}
