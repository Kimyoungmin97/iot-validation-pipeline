package com.iot.pipeline.iot.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Payload 파싱 결과를 담는 내부 DTO
 *
 * 설계 의도:
 * - JSON Map 대신 타입 안전한 클래스로 파싱 결과를 표현
 * - 이후 범위 검사(3단계)에 필요한 정보만 포함해 의존성 최소화
 */
@Getter
@Builder
public class ParsedPayload {

    private final Double value;
    private final String unit;
    private final Long timestamp;
}
