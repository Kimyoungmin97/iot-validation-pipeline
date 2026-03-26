package com.iot.pipeline.iot.exception;

import lombok.Getter;

/**
 * IoT 데이터 검증 실패 시 발생하는 도메인 예외
 *
 * 설계 의도:
 * - 검증 실패 단계(stage)를 예외 객체에 포함해 어느 파이프라인 단계에서 실패했는지 추적 가능
 * - RuntimeException을 상속해 throws 선언 없이 사용 (언체크 예외)
 * - GlobalExceptionHandler에서 일관되게 처리되어 컨트롤러 로직 단순화
 */
@Getter
public class IotValidationException extends RuntimeException {

    private final ValidationStage stage;

    public IotValidationException(ValidationStage stage, String message) {
        super(message);
        this.stage = stage;
    }

    /**
     * 파이프라인 검증 단계 열거형
     * 로그/모니터링에서 어느 단계가 병목인지 분석할 때 활용
     */
    public enum ValidationStage {
        TOPIC_FORMAT,    // 1단계: Topic 규격 검증
        PAYLOAD_PARSE,   // 2단계: Payload 파싱
        RANGE_CHECK      // 3단계: 범위 검사
    }
}
